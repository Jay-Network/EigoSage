package com.jworks.eigosage.data.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveClientTest {

    // -- buildSetupMessage --

    @Test
    fun `buildSetupMessage includes model, voice, and system prompt`() {
        val raw = GeminiLiveClient.buildSetupMessage(
            model = "gemini-3.1-flash-live-preview",
            systemPrompt = "You are Sage.",
            voiceName = "Charon"
        )
        val setup = Json.parseToJsonElement(raw).jsonObject["setup"]!!.jsonObject

        assertEquals("models/gemini-3.1-flash-live-preview", setup["model"]!!.jsonPrimitive.content)

        val generationConfig = setup["generationConfig"]!!.jsonObject
        assertEquals("AUDIO", generationConfig["responseModalities"]!!.jsonArray[0].jsonPrimitive.content)
        val voiceName = generationConfig["speechConfig"]!!.jsonObject
            .get("voiceConfig")!!.jsonObject
            .get("prebuiltVoiceConfig")!!.jsonObject
            .get("voiceName")!!.jsonPrimitive.content
        assertEquals("Charon", voiceName)

        val promptText = setup["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray[0]
            .jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("You are Sage.", promptText)
    }

    // -- buildAudioChunkMessage --

    @Test
    fun `buildAudioChunkMessage wraps base64 payload with pcm 16kHz mimeType`() {
        val raw = GeminiLiveClient.buildAudioChunkMessage("AAAA")
        val audio = Json.parseToJsonElement(raw).jsonObject["realtimeInput"]!!.jsonObject["audio"]!!.jsonObject

        assertEquals(GeminiLiveClient.INPUT_AUDIO_MIME_TYPE, audio["mimeType"]!!.jsonPrimitive.content)
        assertEquals("AAAA", audio["data"]!!.jsonPrimitive.content)
    }

    // -- buildAudioStreamEndMessage --

    @Test
    fun `buildAudioStreamEndMessage sets audioStreamEnd flag`() {
        val raw = GeminiLiveClient.buildAudioStreamEndMessage()
        val realtimeInput = Json.parseToJsonElement(raw).jsonObject["realtimeInput"]!!.jsonObject
        assertTrue(realtimeInput["audioStreamEnd"]!!.jsonPrimitive.boolean)
    }

    // -- buildTextTurnMessage --

    @Test
    fun `buildTextTurnMessage sends a single user turn with turnComplete true by default`() {
        val raw = GeminiLiveClient.buildTextTurnMessage("What does this mean?")
        val clientContent = Json.parseToJsonElement(raw).jsonObject["clientContent"]!!.jsonObject
        val turn = clientContent["turns"]!!.jsonArray[0].jsonObject

        assertEquals("user", turn["role"]!!.jsonPrimitive.content)
        assertEquals("What does this mean?", turn["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertTrue(clientContent["turnComplete"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `buildTextTurnMessage respects turnComplete false`() {
        val raw = GeminiLiveClient.buildTextTurnMessage("partial", turnComplete = false)
        val clientContent = Json.parseToJsonElement(raw).jsonObject["clientContent"]!!.jsonObject
        assertEquals(false, clientContent["turnComplete"]!!.jsonPrimitive.boolean)
    }

    // -- parseServerEvents --

    @Test
    fun `parseServerEvents recognizes setupComplete`() {
        val events = GeminiLiveClient.parseServerEvents("""{"setupComplete": {}}""")
        assertEquals(listOf(LiveEvent.SetupComplete), events)
    }

    @Test
    fun `parseServerEvents extracts text delta from modelTurn parts`() {
        val raw = """{"serverContent": {"modelTurn": {"parts": [{"text": "Hello there"}]}}}"""
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(listOf(LiveEvent.TextDelta("Hello there")), events)
    }

    @Test
    fun `parseServerEvents extracts audio delta from inlineData`() {
        val raw = """{"serverContent": {"modelTurn": {"parts": [
            {"inlineData": {"mimeType": "audio/pcm;rate=24000", "data": "BASE64AUDIO"}}
        ]}}}"""
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(listOf(LiveEvent.AudioDelta("BASE64AUDIO", "audio/pcm;rate=24000")), events)
    }

    @Test
    fun `parseServerEvents combines multiple parts with trailing turnComplete`() {
        val raw = """{"serverContent": {
            "modelTurn": {"parts": [
                {"text": "Sure, "},
                {"inlineData": {"mimeType": "audio/pcm;rate=24000", "data": "AUDIO1"}}
            ]},
            "turnComplete": true
        }}"""
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(
            listOf(
                LiveEvent.TextDelta("Sure, "),
                LiveEvent.AudioDelta("AUDIO1", "audio/pcm;rate=24000"),
                LiveEvent.TurnComplete
            ),
            events
        )
    }

    @Test
    fun `parseServerEvents recognizes interrupted`() {
        val raw = """{"serverContent": {"interrupted": true}}"""
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(listOf(LiveEvent.Interrupted), events)
    }

    @Test
    fun `parseServerEvents falls back to Unrecognized for unknown top-level keys`() {
        val raw = """{"toolCall": {}}"""
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(listOf(LiveEvent.Unrecognized(raw)), events)
    }

    @Test
    fun `parseServerEvents falls back to Unrecognized for malformed JSON`() {
        val raw = "not json"
        val events = GeminiLiveClient.parseServerEvents(raw)
        assertEquals(listOf(LiveEvent.Unrecognized(raw)), events)
    }

    @Test
    fun `parseServerEvents reports goAway as a server error`() {
        val events = GeminiLiveClient.parseServerEvents("""{"goAway": {"timeLeft": "10s"}}""")
        assertEquals(1, events.size)
        assertTrue(events[0] is LiveEvent.ServerError)
    }
}
