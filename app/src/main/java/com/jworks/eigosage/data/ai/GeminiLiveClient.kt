package com.jworks.eigosage.data.ai

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * A parsed event from a Gemini Live (BidiGenerateContent) WebSocket session.
 * One server frame can yield several of these (e.g. a text part, an audio part,
 * and a trailing turnComplete all in the same `serverContent` message).
 */
sealed interface LiveEvent {
    data object SetupComplete : LiveEvent
    data class TextDelta(val text: String) : LiveEvent
    data class AudioDelta(val base64Pcm: String, val mimeType: String) : LiveEvent
    data object TurnComplete : LiveEvent
    data object Interrupted : LiveEvent
    data class ServerError(val message: String) : LiveEvent
    data class Unrecognized(val raw: String) : LiveEvent
}

/**
 * Prototype client for the Gemini Live API (voice + camera reading tutor — see
 * docs/gemini-development-log.md "Gemini Live Agent Candidates"). Covers the WebSocket
 * protocol layer only: session setup, sending audio/text turns, and parsing server events.
 *
 * NOT wired to microphone capture, audio playback, or the UI yet — that requires a real
 * device (AudioRecord/AudioTrack, RECORD_AUDIO permission) and is intentionally left for a
 * separate device-testing pass. Protocol shape confirmed against ai.google.dev/gemini-api/docs
 * live-api pages on 2026-09-04; re-verify if Google revises the API before wiring up audio I/O.
 *
 * Uses its own OkHttp-based [HttpClient] rather than the shared REST client in AiModule —
 * Ktor's Android engine doesn't support the WebSockets plugin.
 */
class GeminiLiveClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val httpClient: HttpClient = HttpClient(OkHttp) { install(WebSockets) }
) {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val WS_HOST = "generativelanguage.googleapis.com"
        private const val WS_PATH =
            "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val DEFAULT_MODEL = "gemini-3.1-flash-live-preview"

        const val INPUT_AUDIO_MIME_TYPE = "audio/pcm;rate=16000"
        const val OUTPUT_AUDIO_MIME_TYPE = "audio/pcm;rate=24000"

        fun buildSetupMessage(model: String, systemPrompt: String, voiceName: String): String {
            return buildJsonObject {
                putJsonObject("setup") {
                    put("model", "models/$model")
                    putJsonObject("generationConfig") {
                        putJsonArray("responseModalities") { add(JsonPrimitive("AUDIO")) }
                        putJsonObject("speechConfig") {
                            putJsonObject("voiceConfig") {
                                putJsonObject("prebuiltVoiceConfig") {
                                    put("voiceName", voiceName)
                                }
                            }
                        }
                    }
                    putJsonObject("systemInstruction") {
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", systemPrompt) })
                        }
                    }
                }
            }.toString()
        }

        fun buildAudioChunkMessage(base64Pcm: String): String {
            return buildJsonObject {
                putJsonObject("realtimeInput") {
                    putJsonObject("audio") {
                        put("mimeType", INPUT_AUDIO_MIME_TYPE)
                        put("data", base64Pcm)
                    }
                }
            }.toString()
        }

        fun buildAudioStreamEndMessage(): String {
            return buildJsonObject {
                putJsonObject("realtimeInput") {
                    put("audioStreamEnd", true)
                }
            }.toString()
        }

        fun buildTextTurnMessage(text: String, turnComplete: Boolean = true): String {
            return buildJsonObject {
                putJsonObject("clientContent") {
                    putJsonArray("turns") {
                        add(buildJsonObject {
                            put("role", "user")
                            putJsonArray("parts") {
                                add(buildJsonObject { put("text", text) })
                            }
                        })
                    }
                    put("turnComplete", turnComplete)
                }
            }.toString()
        }

        /** Parses one server WebSocket frame into zero or more [LiveEvent]s. */
        fun parseServerEvents(raw: String): List<LiveEvent> {
            val json = try {
                Json.parseToJsonElement(raw).jsonObject
            } catch (e: Exception) {
                return listOf(LiveEvent.Unrecognized(raw))
            }

            return when {
                json.containsKey("setupComplete") -> listOf(LiveEvent.SetupComplete)
                json.containsKey("serverContent") ->
                    parseServerContent(json["serverContent"]!!.jsonObject)
                json.containsKey("goAway") -> listOf(LiveEvent.ServerError("Session ending (goAway)"))
                else -> listOf(LiveEvent.Unrecognized(raw))
            }
        }

        private fun parseServerContent(serverContent: JsonObject): List<LiveEvent> {
            val events = mutableListOf<LiveEvent>()

            serverContent["modelTurn"]?.jsonObject
                ?.get("parts")?.jsonArray
                ?.forEach { partElement ->
                    val part = partElement.jsonObject
                    part["text"]?.jsonPrimitive?.contentOrNull?.let {
                        events += LiveEvent.TextDelta(it)
                    }
                    part["inlineData"]?.jsonObject?.let { inlineData ->
                        val data = inlineData["data"]?.jsonPrimitive?.contentOrNull
                        val mimeType = inlineData["mimeType"]?.jsonPrimitive?.contentOrNull
                        if (data != null && mimeType != null) {
                            events += LiveEvent.AudioDelta(data, mimeType)
                        }
                    }
                }

            if (serverContent["interrupted"]?.jsonPrimitive?.booleanOrNull == true) {
                events += LiveEvent.Interrupted
            }
            if (serverContent["turnComplete"]?.jsonPrimitive?.booleanOrNull == true) {
                events += LiveEvent.TurnComplete
            }

            return events
        }
    }

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    private var session: DefaultClientWebSocketSession? = null

    /**
     * Opens a Live session and returns a [Flow] of parsed server events. Collecting the
     * flow drives the receive loop; cancelling collection closes the session.
     */
    fun connect(
        systemPrompt: String,
        voiceName: String = ChatPersona.DEFAULT.liveVoiceName
    ): Flow<LiveEvent> = callbackFlow {
        if (!isAvailable) {
            trySend(LiveEvent.ServerError("Gemini API key not configured"))
            close()
            return@callbackFlow
        }

        val wsSession = try {
            httpClient.webSocketSession(method = HttpMethod.Get, host = WS_HOST, path = WS_PATH) {
                url.parameters.append("key", apiKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Live session", e)
            trySend(LiveEvent.ServerError(e.message ?: "connection failed"))
            close()
            return@callbackFlow
        }
        session = wsSession
        wsSession.send(Frame.Text(buildSetupMessage(model, systemPrompt, voiceName)))

        val receiveJob = launch {
            try {
                for (frame in wsSession.incoming) {
                    if (frame is Frame.Text) {
                        parseServerEvents(frame.readText()).forEach { trySend(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Live session receive loop failed", e)
                trySend(LiveEvent.ServerError(e.message ?: "connection lost"))
            } finally {
                close()
            }
        }

        awaitClose {
            receiveJob.cancel()
            session = null
        }
    }

    suspend fun sendAudioChunk(base64Pcm: String) {
        session?.send(Frame.Text(buildAudioChunkMessage(base64Pcm)))
    }

    suspend fun sendText(text: String, turnComplete: Boolean = true) {
        session?.send(Frame.Text(buildTextTurnMessage(text, turnComplete)))
    }

    suspend fun endAudioStream() {
        session?.send(Frame.Text(buildAudioStreamEndMessage()))
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }
}
