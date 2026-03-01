package com.jworks.eigosage.data.auth

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientFactory {

    private var _instance: SupabaseClient? = null

    fun initialize(supabaseUrl: String, supabaseKey: String, context: Context) {
        if (_instance != null) return

        _instance = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth) {
                sessionManager = SharedPrefsSessionManager(context.applicationContext)
            }
            install(Postgrest)
            install(Functions)
        }
    }

    fun getInstance(): SupabaseClient {
        return _instance ?: throw IllegalStateException(
            "SupabaseClient not initialized. Call initialize() first."
        )
    }

    fun isInitialized(): Boolean = _instance != null
}
