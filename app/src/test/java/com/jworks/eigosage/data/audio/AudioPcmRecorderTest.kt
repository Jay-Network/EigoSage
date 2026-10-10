package com.jworks.eigosage.data.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioPcmRecorderTest {

    @Test
    fun `chunkSizeBytes computes 16-bit mono PCM bytes for default 100ms at 16kHz`() {
        // 16000 samples/sec * 0.1 sec * 2 bytes/sample = 3200 bytes
        assertEquals(3200, AudioPcmRecorder.chunkSizeBytes())
    }

    @Test
    fun `chunkSizeBytes scales with sample rate and duration`() {
        assertEquals(1920, AudioPcmRecorder.chunkSizeBytes(sampleRateHz = 24_000, chunkDurationMs = 40))
    }

    @Test
    fun `encodeChunk base64-encodes without line wrapping`() {
        val pcm = byteArrayOf(0, 1, 2, 3, 4, 5)
        val encoded = AudioPcmRecorder.encodeChunk(pcm)
        assertEquals("AAECAwQF", encoded)
    }

    @Test
    fun `encodeChunk round-trips through AudioPcmPlayer decodeChunk`() {
        val pcm = ByteArray(3200) { (it % 128).toByte() }
        val encoded = AudioPcmRecorder.encodeChunk(pcm)
        val decoded = AudioPcmPlayer.decodeChunk(encoded)
        assertEquals(pcm.toList(), decoded.toList())
    }
}
