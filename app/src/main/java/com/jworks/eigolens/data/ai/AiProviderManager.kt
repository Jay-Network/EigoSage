package com.jworks.eigolens.data.ai

import android.util.Log
import com.jworks.eigolens.domain.ai.AiProvider
import com.jworks.eigolens.domain.ai.AiResponse
import com.jworks.eigolens.domain.ai.AnalysisContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderManager @Inject constructor() {

    companion object {
        private const val TAG = "AiProviderMgr"
    }

    private val providers = mutableMapOf<String, AiProvider>()
    private var activeProviderName: String? = null

    fun registerProvider(provider: AiProvider) {
        providers[provider.name] = provider
        Log.d(TAG, "Registered provider: ${provider.name} (available: ${provider.isAvailable})")
    }

    fun setActiveProvider(name: String) {
        activeProviderName = name
        Log.d(TAG, "Active provider set to: $name")
    }

    val activeProvider: AiProvider?
        get() {
            val name = activeProviderName ?: return providers.values.firstOrNull { it.isAvailable }
            return providers[name]?.takeIf { it.isAvailable }
                ?: providers.values.firstOrNull { it.isAvailable }
        }

    val isAiAvailable: Boolean
        get() = activeProvider != null

    val availableProviders: List<String>
        get() = providers.values.filter { it.isAvailable }.map { it.name }

    val allProviderNames: List<String>
        get() = providers.keys.toList()

    suspend fun analyze(context: AnalysisContext): Result<AiResponse> {
        val provider = activeProvider
            ?: return Result.failure(IllegalStateException("No AI provider available. Add an API key in Settings."))
        return provider.analyze(context)
    }
}
