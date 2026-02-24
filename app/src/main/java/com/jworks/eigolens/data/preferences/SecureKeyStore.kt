package com.jworks.eigolens.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jworks.eigolens.BuildConfig

/**
 * Encrypted storage for sensitive data like API keys.
 * Uses EncryptedSharedPreferences backed by Android Keystore.
 */
class SecureKeyStore(context: Context) {

    companion object {
        private const val TAG = "SecureKeyStore"
        private const val FILE_NAME = "eigolens_secure_prefs"
        private const val KEY_CLAUDE_API_KEY = "claude_api_key"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    private val prefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create encrypted prefs", e)
        null
    }

    fun getClaudeApiKey(): String {
        val saved = prefs?.getString(KEY_CLAUDE_API_KEY, "") ?: ""
        return saved.ifBlank { BuildConfig.CLAUDE_API_KEY }
    }

    fun setClaudeApiKey(key: String) {
        prefs?.edit()?.putString(KEY_CLAUDE_API_KEY, key)?.apply()
    }

    fun getGeminiApiKey(): String {
        val saved = prefs?.getString(KEY_GEMINI_API_KEY, "") ?: ""
        return saved.ifBlank { BuildConfig.GEMINI_API_KEY }
    }

    fun setGeminiApiKey(key: String) {
        prefs?.edit()?.putString(KEY_GEMINI_API_KEY, key)?.apply()
    }
}
