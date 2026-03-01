package com.jworks.eigosage.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigosage.data.ai.AiProviderManager
import com.jworks.eigosage.data.preferences.SecureKeyStore
import com.jworks.eigosage.domain.models.AppSettings
import com.jworks.eigosage.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val secureKeyStore: SecureKeyStore,
    private val aiProviderManager: AiProviderManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _claudeApiKey = MutableStateFlow(secureKeyStore.getClaudeApiKey())
    val claudeApiKey: StateFlow<String> = _claudeApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(secureKeyStore.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _activeProvider = MutableStateFlow(aiProviderManager.activeProvider?.name)
    val activeProvider: StateFlow<String?> = _activeProvider.asStateFlow()

    private val _availableProviders = MutableStateFlow(aiProviderManager.availableProviders)
    val availableProviders: StateFlow<List<String>> = _availableProviders.asStateFlow()

    private val _allProviderNames = MutableStateFlow(aiProviderManager.allProviderNames)
    val allProviderNames: StateFlow<List<String>> = _allProviderNames.asStateFlow()

    fun setClaudeApiKey(key: String) {
        secureKeyStore.setClaudeApiKey(key)
        _claudeApiKey.value = key
        // Re-register provider with new key
        aiProviderManager.registerProvider(
            com.jworks.eigosage.data.ai.ClaudeProvider(
                httpClient = getHttpClient(),
                apiKey = key
            )
        )
        refreshProviderState()
    }

    fun setGeminiApiKey(key: String) {
        secureKeyStore.setGeminiApiKey(key)
        _geminiApiKey.value = key
        aiProviderManager.registerProvider(
            com.jworks.eigosage.data.ai.GeminiProvider(
                httpClient = getHttpClient(),
                apiKey = key
            )
        )
        refreshProviderState()
    }

    fun setIpaFontScale(scale: Float) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            settingsRepository.updateSettings(current.copy(ipaFontScale = scale))
        }
    }

    fun resetTokenUsage(provider: String?) {
        viewModelScope.launch {
            settingsRepository.resetTokenUsage(provider)
        }
    }

    fun setActiveProvider(name: String) {
        aiProviderManager.setActiveProvider(name)
        refreshProviderState()
    }

    private fun refreshProviderState() {
        _allProviderNames.value = aiProviderManager.allProviderNames
        _availableProviders.value = aiProviderManager.availableProviders
        _activeProvider.value = aiProviderManager.activeProvider?.name
    }

    private fun getHttpClient(): io.ktor.client.HttpClient {
        // Reuse the same engine config as AiModule
        return io.ktor.client.HttpClient(io.ktor.client.engine.android.Android) {
            engine {
                connectTimeout = 15_000
                socketTimeout = 60_000
            }
        }
    }
}
