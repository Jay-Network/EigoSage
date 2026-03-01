package com.jworks.eigosage.di

import android.content.Context
import com.jworks.eigosage.data.ai.AiProviderManager
import com.jworks.eigosage.data.ai.ClaudeProvider
import com.jworks.eigosage.data.ai.GeminiChatClient
import com.jworks.eigosage.data.ai.GeminiOcrCorrector
import com.jworks.eigosage.data.ai.GeminiProvider
import com.jworks.eigosage.data.preferences.SecureKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideSecureKeyStore(@ApplicationContext context: Context): SecureKeyStore {
        return SecureKeyStore(context)
    }

    @Provides
    @Singleton
    fun provideAiHttpClient(): HttpClient {
        return HttpClient(Android) {
            engine {
                connectTimeout = 15_000
                socketTimeout = 60_000
            }
        }
    }

    @Provides
    @Singleton
    fun provideAiProviderManager(
        httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): AiProviderManager {
        val manager = AiProviderManager()

        manager.registerProvider(
            ClaudeProvider(httpClient, secureKeyStore.getClaudeApiKey())
        )
        manager.registerProvider(
            GeminiProvider(httpClient, secureKeyStore.getGeminiApiKey())
        )

        return manager
    }

    @Provides
    @Singleton
    fun provideGeminiChatClient(
        httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): GeminiChatClient {
        return GeminiChatClient(httpClient, secureKeyStore.getGeminiApiKey())
    }

    @Provides
    @Singleton
    fun provideGeminiOcrCorrector(
        httpClient: HttpClient,
        secureKeyStore: SecureKeyStore
    ): GeminiOcrCorrector {
        return GeminiOcrCorrector(httpClient, secureKeyStore.getGeminiApiKey())
    }
}
