package com.jworks.eigosage.data.repository

import com.jworks.eigosage.data.preferences.SettingsDataStore
import com.jworks.eigosage.domain.models.AppSettings
import com.jworks.eigosage.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.updateSettings(settings)
    }

    override suspend fun addTokenUsage(provider: String, inputTokens: Int, outputTokens: Int) {
        dataStore.addTokenUsage(provider, inputTokens, outputTokens)
    }

    override suspend fun resetTokenUsage(provider: String?) {
        dataStore.resetTokenUsage(provider)
    }
}
