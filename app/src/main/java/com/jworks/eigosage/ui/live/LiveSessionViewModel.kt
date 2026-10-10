package com.jworks.eigosage.ui.live

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigosage.data.ai.ChatPersona
import com.jworks.eigosage.data.ai.GeminiChatClient
import com.jworks.eigosage.data.ai.GeminiLiveClient
import com.jworks.eigosage.data.ai.LiveEvent
import com.jworks.eigosage.data.ai.ScanMode
import com.jworks.eigosage.data.audio.AudioPcmPlayer
import com.jworks.eigosage.data.audio.AudioPcmRecorder
import com.jworks.eigosage.data.preferences.SecureKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LiveConnectionState { IDLE, CONNECTING, LISTENING, ERROR }

data class LiveSessionUiState(
    val connectionState: LiveConnectionState = LiveConnectionState.IDLE,
    val persona: ChatPersona = ChatPersona.DEFAULT,
    val transcript: List<String> = emptyList(),
    val errorMessage: String? = null
)

private const val DEFAULT_CEFR_LEVEL = "B1"

/**
 * Orchestrates a Gemini Live voice session: mic capture -> [GeminiLiveClient] -> speaker
 * playback, plus a running text transcript of the model's spoken responses (Gemini sends
 * both audio and its text transcript in modelTurn parts).
 *
 * UNVERIFIED on a real device as of 2026-10-09 — see AudioPcmRecorder/AudioPcmPlayer.
 */
@HiltViewModel
class LiveSessionViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveSessionUiState())
    val uiState: StateFlow<LiveSessionUiState> = _uiState.asStateFlow()

    private val recorder = AudioPcmRecorder()
    private val player = AudioPcmPlayer()

    private var liveClient: GeminiLiveClient? = null
    private var sessionJob: Job? = null
    private var recordingJob: Job? = null

    fun selectPersona(persona: ChatPersona) {
        _uiState.update { it.copy(persona = persona) }
    }

    /** Opens a Live session with the current persona and starts streaming mic audio into it. */
    fun startSession() {
        if (_uiState.value.connectionState != LiveConnectionState.IDLE &&
            _uiState.value.connectionState != LiveConnectionState.ERROR
        ) {
            return
        }

        val client = GeminiLiveClient(apiKey = secureKeyStore.getGeminiApiKey())
        liveClient = client

        if (!client.isAvailable) {
            _uiState.update {
                it.copy(connectionState = LiveConnectionState.ERROR, errorMessage = "Gemini API key not configured")
            }
            return
        }

        _uiState.update { it.copy(connectionState = LiveConnectionState.CONNECTING, errorMessage = null, transcript = emptyList()) }

        val persona = _uiState.value.persona
        val systemPrompt = GeminiChatClient.buildCefrSystemPrompt(DEFAULT_CEFR_LEVEL, persona, ScanMode.DEFAULT)

        sessionJob = viewModelScope.launch {
            client.connect(systemPrompt = systemPrompt, voiceName = persona.liveVoiceName)
                .collect { event -> handleLiveEvent(event) }
        }
    }

    private fun handleLiveEvent(event: LiveEvent) {
        when (event) {
            is LiveEvent.SetupComplete -> {
                _uiState.update { it.copy(connectionState = LiveConnectionState.LISTENING) }
                startMicStreaming()
            }
            is LiveEvent.TextDelta -> {
                _uiState.update { it.copy(transcript = it.transcript + event.text) }
            }
            is LiveEvent.AudioDelta -> {
                player.play(event.base64Pcm)
            }
            is LiveEvent.TurnComplete, is LiveEvent.Interrupted -> {
                // No state change needed — audio/text already streamed as it arrived.
            }
            is LiveEvent.ServerError -> {
                _uiState.update {
                    it.copy(connectionState = LiveConnectionState.ERROR, errorMessage = event.message)
                }
                stopSession()
            }
            is LiveEvent.Unrecognized -> {
                Log.w("LiveSessionViewModel", "Unrecognized server message: ${event.raw}")
            }
        }
    }

    private fun startMicStreaming() {
        recordingJob = viewModelScope.launch {
            recorder.start().collect { base64Chunk ->
                liveClient?.sendAudioChunk(base64Chunk)
            }
        }
    }

    /** Stops mic capture, closes the Live session, and releases audio resources. */
    fun stopSession() {
        recordingJob?.cancel()
        recordingJob = null
        sessionJob?.cancel()
        sessionJob = null
        viewModelScope.launch { liveClient?.disconnect() }
        liveClient = null
        player.release()
        _uiState.update { it.copy(connectionState = LiveConnectionState.IDLE) }
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
