package com.jworks.eigosage.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jworks.eigosage.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eigosage_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val HIGHLIGHT_COLOR = longPreferencesKey("highlight_color")
        val STROKE_WIDTH = floatPreferencesKey("stroke_width")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val CEFR_THRESHOLD = stringPreferencesKey("cefr_threshold")
        val SHOW_IPA_OVERLAY = booleanPreferencesKey("show_ipa_overlay")
        val IPA_FONT_SCALE = floatPreferencesKey("ipa_font_scale")
        val CLAUDE_INPUT_TOKENS = longPreferencesKey("claude_input_tokens")
        val CLAUDE_OUTPUT_TOKENS = longPreferencesKey("claude_output_tokens")
        val GEMINI_INPUT_TOKENS = longPreferencesKey("gemini_input_tokens")
        val GEMINI_OUTPUT_TOKENS = longPreferencesKey("gemini_output_tokens")
    }

    private val defaults = AppSettings()

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            highlightColor = prefs[Keys.HIGHLIGHT_COLOR] ?: defaults.highlightColor,
            strokeWidth = prefs[Keys.STROKE_WIDTH] ?: defaults.strokeWidth,
            aiProvider = prefs[Keys.AI_PROVIDER] ?: defaults.aiProvider,
            aiModel = prefs[Keys.AI_MODEL] ?: defaults.aiModel,
            cefrThreshold = prefs[Keys.CEFR_THRESHOLD] ?: defaults.cefrThreshold,
            showIpaOverlay = prefs[Keys.SHOW_IPA_OVERLAY] ?: defaults.showIpaOverlay,
            ipaFontScale = prefs[Keys.IPA_FONT_SCALE] ?: defaults.ipaFontScale,
            claudeInputTokens = prefs[Keys.CLAUDE_INPUT_TOKENS] ?: defaults.claudeInputTokens,
            claudeOutputTokens = prefs[Keys.CLAUDE_OUTPUT_TOKENS] ?: defaults.claudeOutputTokens,
            geminiInputTokens = prefs[Keys.GEMINI_INPUT_TOKENS] ?: defaults.geminiInputTokens,
            geminiOutputTokens = prefs[Keys.GEMINI_OUTPUT_TOKENS] ?: defaults.geminiOutputTokens
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGHLIGHT_COLOR] = settings.highlightColor
            prefs[Keys.STROKE_WIDTH] = settings.strokeWidth
            prefs[Keys.AI_PROVIDER] = settings.aiProvider
            prefs[Keys.AI_MODEL] = settings.aiModel
            prefs[Keys.CEFR_THRESHOLD] = settings.cefrThreshold
            prefs[Keys.SHOW_IPA_OVERLAY] = settings.showIpaOverlay
            prefs[Keys.IPA_FONT_SCALE] = settings.ipaFontScale
            prefs[Keys.CLAUDE_INPUT_TOKENS] = settings.claudeInputTokens
            prefs[Keys.CLAUDE_OUTPUT_TOKENS] = settings.claudeOutputTokens
            prefs[Keys.GEMINI_INPUT_TOKENS] = settings.geminiInputTokens
            prefs[Keys.GEMINI_OUTPUT_TOKENS] = settings.geminiOutputTokens
        }
    }

    suspend fun addTokenUsage(provider: String, inputTokens: Int, outputTokens: Int) {
        context.dataStore.edit { prefs ->
            when (provider) {
                "Claude" -> {
                    prefs[Keys.CLAUDE_INPUT_TOKENS] = (prefs[Keys.CLAUDE_INPUT_TOKENS] ?: 0L) + inputTokens
                    prefs[Keys.CLAUDE_OUTPUT_TOKENS] = (prefs[Keys.CLAUDE_OUTPUT_TOKENS] ?: 0L) + outputTokens
                }
                "Gemini" -> {
                    prefs[Keys.GEMINI_INPUT_TOKENS] = (prefs[Keys.GEMINI_INPUT_TOKENS] ?: 0L) + inputTokens
                    prefs[Keys.GEMINI_OUTPUT_TOKENS] = (prefs[Keys.GEMINI_OUTPUT_TOKENS] ?: 0L) + outputTokens
                }
            }
        }
    }

    suspend fun resetTokenUsage(provider: String?) {
        context.dataStore.edit { prefs ->
            when (provider) {
                "Claude" -> {
                    prefs[Keys.CLAUDE_INPUT_TOKENS] = 0L
                    prefs[Keys.CLAUDE_OUTPUT_TOKENS] = 0L
                }
                "Gemini" -> {
                    prefs[Keys.GEMINI_INPUT_TOKENS] = 0L
                    prefs[Keys.GEMINI_OUTPUT_TOKENS] = 0L
                }
                null -> {
                    prefs[Keys.CLAUDE_INPUT_TOKENS] = 0L
                    prefs[Keys.CLAUDE_OUTPUT_TOKENS] = 0L
                    prefs[Keys.GEMINI_INPUT_TOKENS] = 0L
                    prefs[Keys.GEMINI_OUTPUT_TOKENS] = 0L
                }
            }
        }
    }
}
