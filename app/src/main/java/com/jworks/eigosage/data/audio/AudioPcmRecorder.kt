package com.jworks.eigosage.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Captures microphone audio as base64-encoded 16-bit PCM chunks matching the Gemini Live API's
 * required input format ([GeminiLiveClient.INPUT_AUDIO_MIME_TYPE] — 16kHz, mono, little-endian).
 *
 * Caller must hold `RECORD_AUDIO` before calling [start] — this class does not check it.
 * UNVERIFIED on a real device as of 2026-10-09: the chunking/encoding logic is unit-tested,
 * but actual microphone capture behavior has not been observed running.
 */
class AudioPcmRecorder {

    companion object {
        private const val TAG = "AudioPcmRecorder"
        const val SAMPLE_RATE_HZ = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_DURATION_MS = 100
        private const val BYTES_PER_SAMPLE = 2 // 16-bit PCM

        /** Bytes of PCM data in one [CHUNK_DURATION_MS]-long chunk at [SAMPLE_RATE_HZ], mono. */
        fun chunkSizeBytes(sampleRateHz: Int = SAMPLE_RATE_HZ, chunkDurationMs: Int = CHUNK_DURATION_MS): Int {
            val samplesPerChunk = sampleRateHz * chunkDurationMs / 1000
            return samplesPerChunk * BYTES_PER_SAMPLE
        }

        /** Base64-encodes a raw PCM chunk for [GeminiLiveClient.sendAudioChunk]. */
        fun encodeChunk(pcm: ByteArray): String = Base64.encodeToString(pcm, Base64.NO_WRAP)
    }

    private var audioRecord: AudioRecord? = null

    /**
     * Starts recording and emits base64-encoded PCM chunks roughly every [CHUNK_DURATION_MS]ms.
     * Cancelling collection of the returned flow stops and releases the recorder.
     */
    @SuppressLint("MissingPermission")
    fun start(): Flow<String> = callbackFlow {
        val chunkBytes = chunkSizeBytes()
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            close(IllegalStateException("AudioRecord.getMinBufferSize returned $minBufferSize — unsupported config"))
            return@callbackFlow
        }

        val recordBufferSize = maxOf(minBufferSize, chunkBytes * 2)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            recordBufferSize
        )
        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord failed to initialize"))
            return@callbackFlow
        }

        record.startRecording()
        val buffer = ByteArray(chunkBytes)

        while (isActive) {
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0) {
                val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                trySend(encodeChunk(chunk))
            } else if (read < 0) {
                Log.e(TAG, "AudioRecord.read returned error code $read")
                break
            }
        }

        awaitClose {
            try {
                record.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioRecord.stop() on non-recording record", e)
            }
            record.release()
            audioRecord = null
        }
    }.flowOn(Dispatchers.IO)
}
