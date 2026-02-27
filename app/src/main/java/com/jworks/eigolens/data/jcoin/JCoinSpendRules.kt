package com.jworks.eigolens.data.jcoin

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EigoLens J Coin spend rules — daily free quotas, streak freezes, theme unlocks.
 *
 * SPEND ITEMS:
 * | Item                    | Cost     | Type       | Daily Free |
 * |-------------------------|----------|------------|------------|
 * | AI Deep Analysis        | 8 coins  | Consumable | 3/day      |
 * | CEFR Progress Report    | 10 coins | Consumable | —          |
 * | Export Difficult Words  | 15 coins | Consumable | —          |
 * | Streak Freeze           | 20 coins | Consumable | —          |
 * | Theme Unlock            | 30-60    | Permanent  | —          |
 */
@Singleton
class JCoinSpendRules @Inject constructor() {

    companion object {
        private const val PREFS_NAME = "eigolens_jcoin"

        // Daily free AI analysis
        private const val KEY_FREE_AI_ANALYSIS_DATE = "free_ai_analysis_date"
        private const val KEY_FREE_AI_ANALYSIS_COUNT = "free_ai_analysis_count"
        private const val FREE_AI_ANALYSIS_DAILY_CAP = 3

        // Streak freeze
        private const val KEY_STREAK_FREEZES_OWNED = "streak_freezes_owned"

        // Theme unlock prefix
        private const val KEY_THEME_PREFIX = "theme_unlocked_"
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // -- Daily Free AI Analysis --

    private fun ensureFreeAiDate(context: Context) {
        val prefs = getPrefs(context)
        val today = java.time.LocalDate.now().toString()
        if (prefs.getString(KEY_FREE_AI_ANALYSIS_DATE, null) != today) {
            prefs.edit()
                .putString(KEY_FREE_AI_ANALYSIS_DATE, today)
                .putInt(KEY_FREE_AI_ANALYSIS_COUNT, 0)
                .apply()
        }
    }

    /** Returns true if the user still has free AI analyses remaining today */
    fun checkFreeAiAnalysis(context: Context): Boolean {
        ensureFreeAiDate(context)
        val count = getPrefs(context).getInt(KEY_FREE_AI_ANALYSIS_COUNT, 0)
        return count < FREE_AI_ANALYSIS_DAILY_CAP
    }

    /** Consume one free AI analysis. Call after confirming checkFreeAiAnalysis() was true. */
    fun consumeFreeAiAnalysis(context: Context) {
        ensureFreeAiDate(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_FREE_AI_ANALYSIS_COUNT, 0)
        prefs.edit().putInt(KEY_FREE_AI_ANALYSIS_COUNT, count + 1).apply()
    }

    /** How many free AI analyses remain today */
    fun freeAiAnalysisRemaining(context: Context): Int {
        ensureFreeAiDate(context)
        val count = getPrefs(context).getInt(KEY_FREE_AI_ANALYSIS_COUNT, 0)
        return (FREE_AI_ANALYSIS_DAILY_CAP - count).coerceAtLeast(0)
    }

    // -- Streak Freeze --

    fun getStreakFreezesOwned(context: Context): Int {
        return getPrefs(context).getInt(KEY_STREAK_FREEZES_OWNED, 0)
    }

    fun purchaseStreakFreeze(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_STREAK_FREEZES_OWNED, 0)
        prefs.edit().putInt(KEY_STREAK_FREEZES_OWNED, current + 1).apply()
    }

    /** Consume a streak freeze. Returns true if one was available. */
    fun consumeStreakFreeze(context: Context): Boolean {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_STREAK_FREEZES_OWNED, 0)
        if (current <= 0) return false
        prefs.edit().putInt(KEY_STREAK_FREEZES_OWNED, current - 1).apply()
        return true
    }

    // -- Theme Unlocks --

    fun isThemeUnlocked(context: Context, themeId: String): Boolean {
        return getPrefs(context).getBoolean(KEY_THEME_PREFIX + themeId, false)
    }

    fun unlockTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putBoolean(KEY_THEME_PREFIX + themeId, true).apply()
    }

    fun getUnlockedThemes(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.all.keys
            .filter { it.startsWith(KEY_THEME_PREFIX) && prefs.getBoolean(it, false) }
            .map { it.removePrefix(KEY_THEME_PREFIX) }
            .toSet()
    }
}
