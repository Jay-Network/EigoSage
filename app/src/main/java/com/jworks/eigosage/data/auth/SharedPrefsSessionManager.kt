package com.jworks.eigosage.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPrefsSessionManager(context: Context) : SessionManager {

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "eigosage_supabase_session"
        private const val KEY_SESSION = "session_json"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        try {
            val serialized = json.encodeToString(session)
            prefs.edit().putString(KEY_SESSION, serialized).apply()
            Log.d(TAG, "Session saved")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save session: ${e.message}")
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val serialized = prefs.getString(KEY_SESSION, null) ?: return null
            json.decodeFromString<UserSession>(serialized).also {
                Log.d(TAG, "Session loaded")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load session: ${e.message}")
            deleteSession()
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
        Log.d(TAG, "Session deleted")
    }
}
