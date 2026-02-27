package com.jworks.eigolens.data.jcoin

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Simplified auth for EigoLens J Coin Phase 1.
 * Uses device UUID as identity. Gets Supabase access token if available.
 * Edge Functions accept the anon key in the Authorization header as fallback.
 */
@Singleton
class DeviceAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("jcoin") private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "DeviceAuth"
        private const val PREFS_NAME = "eigolens_device_auth"
        private const val KEY_DEVICE_UUID = "device_uuid"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Get or create a stable device UUID */
    fun getDeviceId(): String {
        var uuid = prefs.getString(KEY_DEVICE_UUID, null)
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
            Log.d(TAG, "Generated new device UUID: $uuid")
        }
        return uuid
    }

    /** Get Supabase access token for J Coin API calls. Returns null if unavailable. */
    fun getAccessToken(): String? {
        return try {
            supabaseClient.auth.currentAccessTokenOrNull()
        } catch (e: Exception) {
            Log.d(TAG, "No access token available: ${e.message}")
            null
        }
    }

    /** Ensure device UUID exists and Supabase client is ready */
    suspend fun ensureSession() {
        // Ensure device UUID is generated
        getDeviceId()
        Log.d(TAG, "Device session ready, UUID=${getDeviceId()}")
    }
}
