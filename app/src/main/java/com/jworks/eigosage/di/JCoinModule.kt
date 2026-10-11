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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JCoinModule {

    private const val JCOIN_SUPABASE_URL = "https://j-coin.jworks-ai.com"
    private const val JCOIN_SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl0dnRneWNmdXV4ZG9reW5ucG1qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUxNzY3NDEsImV4cCI6MjEwMDUzNjc0MX0.1QtkmLpoTMR5aQOYox3NH8XojPj7KzV3iDh2DBifRQw"
    const val JCOIN_APP_KEY =
        "b1d1d811477c7bff6138df04a612ef2b6d2f21851c120249bf3bbfa91c6b77a1"

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
