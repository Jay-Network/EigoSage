package com.jworks.eigosage

import android.app.Application
import android.util.Log
import com.jworks.eigosage.data.auth.SupabaseClientFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class EigoSageApplication : Application() {

    companion object {
        private const val TAG = "EigoSage"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeSupabase()
    }

    private fun initializeSupabase() {
        val url = BuildConfig.AUTH_SUPABASE_URL
        val key = BuildConfig.AUTH_SUPABASE_ANON_KEY

        if (url.isNotBlank() && key.isNotBlank()) {
            try {
                SupabaseClientFactory.initialize(url, key, this)
                Log.i(TAG, "Supabase initialized (session persistence enabled)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize Supabase: ${e.message}")
            }
        } else {
            Log.i(TAG, "Supabase credentials not configured - running without auth")
        }
    }
}
