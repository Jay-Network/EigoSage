package com.jworks.eigosage.di

import com.jworks.eigosage.data.jcoin.DeviceAuthRepository
import com.jworks.eigosage.data.jcoin.JCoinClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JCoinModule {

    private const val JCOIN_SUPABASE_URL = "https://inygcrdhfmoerborxehq.supabase.co"
    private const val JCOIN_SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlueWdjcmRoZm1vZXJib3J4ZWhxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njk0OTQyNDMsImV4cCI6MjA4NTA3MDI0M30.zQEMcvdGoX1Uk1rJRIeJSu1EAzu4XeuxkdnsqNADOGU"
    const val JCOIN_APP_KEY =
        "532c26a03a18ebe482728ceee49d89a24fe01ac330b604586d7514426ccb4070"

    @Provides
    @Singleton
    @Named("jcoin")
    fun provideJCoinSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = JCOIN_SUPABASE_URL,
            supabaseKey = JCOIN_SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Functions)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideJCoinClient(
        @Named("jcoin") supabaseClient: SupabaseClient,
        deviceAuthRepository: DeviceAuthRepository
    ): JCoinClient {
        return JCoinClient(supabaseClient, deviceAuthRepository)
    }
}
