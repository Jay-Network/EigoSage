package com.jworks.eigolens.ui.capture

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigolens.data.repository.DefinitionRepository
import com.jworks.eigolens.domain.analysis.ReadabilityCalculator
import com.jworks.eigolens.domain.models.Definition
import com.jworks.eigolens.domain.models.ReadabilityMetrics
import com.jworks.eigolens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CaptureState {
    data object Camera : CaptureState()
    data class Annotate(val capturedImage: CapturedImage) : CaptureState()
}

data class CapturedImage(
    val bitmap: Bitmap,
    val ocrResult: com.jworks.eigolens.domain.models.OCRResult,
    val timestamp: Long
)

enum class InteractionMode { TAP, CIRCLE }

sealed class PanelState {
    data object Idle : PanelState()
    data object Loading : PanelState()
    data class WordDefinition(val definition: Definition) : PanelState()
    data class ReadabilityResult(val metrics: ReadabilityMetrics) : PanelState()
    data class NotFound(val word: String) : PanelState()
    data class Error(val message: String) : PanelState()
}

@HiltViewModel
class CaptureFlowViewModel @Inject constructor(
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val definitionRepository: DefinitionRepository,
    private val readabilityCalculator: ReadabilityCalculator
) : ViewModel() {

    companion object {
        private const val TAG = "CaptureFlowVM"
        private const val MAX_BITMAP_DIMENSION = 2048
    }

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Camera)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _panelState = MutableStateFlow<PanelState>(PanelState.Idle)
    val panelState: StateFlow<PanelState> = _panelState.asStateFlow()

    private val _interactionMode = MutableStateFlow(InteractionMode.TAP)
    val interactionMode: StateFlow<InteractionMode> = _interactionMode.asStateFlow()

    private val _tappedWord = MutableStateFlow<TapResult?>(null)
    val tappedWord: StateFlow<TapResult?> = _tappedWord.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    init {
        viewModelScope.launch {
            definitionRepository.preloadCommonWords(100)
        }
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun setInteractionMode(mode: InteractionMode) {
        _interactionMode.value = mode
    }

    fun onPhotoCapture(bitmap: Bitmap) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val scaled = downscaleBitmap(bitmap)
                val ocrResult = processCameraFrame.processStaticImage(scaled)
                Log.d(TAG, "Captured: ${ocrResult.texts.size} lines, " +
                        "${ocrResult.texts.sumOf { it.elements.size }} words, " +
                        "${ocrResult.processingTimeMs}ms")
                _captureState.value = CaptureState.Annotate(
                    CapturedImage(scaled, ocrResult, System.currentTimeMillis())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Capture failed", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onGalleryImport(bitmap: Bitmap) {
        onPhotoCapture(bitmap)
    }

    /** Called when user taps a word in TAP mode */
    fun onWordTapped(tapResult: TapResult) {
        _tappedWord.value = tapResult
        // Strip punctuation from word before lookup
        val cleanWord = tapResult.word.replace(Regex("[^a-zA-Z'-]"), "").trim()
        if (cleanWord.isEmpty()) return
        lookupWord(cleanWord)
    }

    /** Called when user circles words in CIRCLE mode (lasso selection) */
    fun selectWords(words: List<String>) {
        if (words.isEmpty()) return
        _tappedWord.value = null

        // For Phase A: all selections go through local word lookup
        // Phase B will add scope detection: 1 word = local, 2-8 = AI phrase, 9+ = AI paragraph
        val query = if (words.size == 1) words.first() else words.joinToString(" ")
        lookupWord(query, fallbackWord = if (words.size > 1) words.first() else null)
    }

    fun analyzeReadability() {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return
        val text = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        if (text.isBlank()) return

        val metrics = readabilityCalculator.calculate(text) ?: return
        _panelState.value = PanelState.ReadabilityResult(metrics)
    }

    fun switchToWordLookup() {
        _panelState.value = PanelState.Idle
    }

    fun resetToCamera() {
        _captureState.value = CaptureState.Camera
        _panelState.value = PanelState.Idle
        _interactionMode.value = InteractionMode.TAP
        _tappedWord.value = null
    }

    private fun lookupWord(query: String, fallbackWord: String? = null) {
        viewModelScope.launch {
            _panelState.value = PanelState.Loading

            definitionRepository.getDefinition(query)
                .onSuccess { _panelState.value = PanelState.WordDefinition(it) }
                .onFailure {
                    if (fallbackWord != null) {
                        definitionRepository.getDefinition(fallbackWord)
                            .onSuccess { _panelState.value = PanelState.WordDefinition(it) }
                            .onFailure { err -> setFailureState(fallbackWord, err) }
                    } else {
                        setFailureState(query, it)
                    }
                }
        }
    }

    private fun setFailureState(word: String, error: Throwable) {
        _panelState.value = if (error is NoSuchElementException) {
            PanelState.NotFound(word)
        } else {
            PanelState.Error(error.message ?: "An error occurred")
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= MAX_BITMAP_DIMENSION) return bitmap
        val scale = MAX_BITMAP_DIMENSION.toFloat() / maxDim
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }
}
