package com.jworks.eigosage.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log

/**
 * Plays base64-encoded 16-bit PCM audio returned by the Gemini Live API
 * ([GeminiLiveClient.OUTPUT_AUDIO_MIME_TYPE] — 24kHz, mono, little-endian).
 *
 * UNVERIFIED on a real device as of 2026-10-09: decoding is unit-tested, but actual playback
 * has not been observed running.
 */
class AudioPcmPlayer {

    companion object {
        private const val TAG = "AudioPcmPlayer"
        const val SAMPLE_RATE_HZ = 24_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** Decodes one [GeminiLiveClient] AudioDelta payload back into raw PCM bytes. */
        fun decodeChunk(base64Pcm: String): ByteArray = Base64.decode(base64Pcm, Base64.NO_WRAP)
    }

    private var audioTrack: AudioTrack? = null

    private fun ensureTrack(): AudioTrack {
        audioTrack?.let { return it }

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build(),
            maxOf(minBufferSize, 1),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()
        audioTrack = track
        return track
    }

    /** Decodes and queues one audio chunk for playback. Starts the track on first call. */
    fun play(base64Pcm: String) {
        val pcm = decodeChunk(base64Pcm)
        val track = ensureTrack()
        val written = track.write(pcm, 0, pcm.size)
        if (written < 0) {
            Log.e(TAG, "AudioTrack.write returned error code $written")
        }
    }

    /** Stops and releases the underlying [AudioTrack]. Safe to call multiple times. */
    fun release() {
        audioTrack?.let { track ->
            try {
                track.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioTrack.stop() on non-playing track", e)
            }
            track.release()
        }
        audioTrack = null
    }
}
