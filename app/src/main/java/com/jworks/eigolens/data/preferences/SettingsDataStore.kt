package com.jworks.eigolens.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jworks.eigolens.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eigolens_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val HIGHLIGHT_COLOR = longPreferencesKey("highlight_color")
        val STROKE_WIDTH = floatPreferencesKey("stroke_width")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
    }

    private val defaults = AppSettings()

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            highlightColor = prefs[Keys.HIGHLIGHT_COLOR] ?: defaults.highlightColor,
            strokeWidth = prefs[Keys.STROKE_WIDTH] ?: defaults.strokeWidth,
            aiProvider = prefs[Keys.AI_PROVIDER] ?: defaults.aiProvider,
            aiModel = prefs[Keys.AI_MODEL] ?: defaults.aiModel
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGHLIGHT_COLOR] = settings.highlightColor
            prefs[Keys.STROKE_WIDTH] = settings.strokeWidth
            prefs[Keys.AI_PROVIDER] = settings.aiProvider
            prefs[Keys.AI_MODEL] = settings.aiModel
        }
    }
}
