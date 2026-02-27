package com.jworks.eigolens.data.jcoin

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EigoLens J Coin earn rules with daily caps.
 *
 * ENGAGEMENT (200/mo backend cap):
 * | Action                      | Coins | Cap/day     |
 * |-----------------------------|-------|-------------|
 * | Daily login                 | 2     | 1/day       |
 * | Scan text                   | 3     | No daily cap|
 * | Difficult word reviewed     | 1     | 15/day      |
 * | All difficult words cleared | 5     | Per scan    |
 * | CEFR threshold adjusted     | 1     | 3/day       |
 * | Save definition (bookmark)  | 2     | 10/day      |
 * | Sentence analysis           | 3     | 5/day       |
 *
 * MILESTONES (uncapped, one-time):
 * | Milestone               | Coins |
 * |-------------------------|-------|
 * | First CEFR scan         | 10    |
 * | First threshold set     | 5     |
 * | 50 CEFR words reviewed  | 25    |
 * | 200 CEFR words reviewed | 60    |
 * | 500 CEFR words reviewed | 100   |
 * | 50 scans                | 25    |
 * | 200 scans               | 60    |
 * | 7-day streak            | 20    |
 * | 30-day streak           | 50    |
 * | 90-day streak           | 100   |
 */
@Singleton
class JCoinEarnRules @Inject constructor() {

    companion object {
        private const val PREFS_NAME = "eigolens_jcoin"
        private const val KEY_DATE = "jcoin_date"
        private const val KEY_DAILY_LOGIN_CLAIMED = "daily_login_claimed"
        private const val KEY_DIFFICULT_WORD_COUNT = "difficult_word_count"
        private const val KEY_THRESHOLD_ADJUST_COUNT = "threshold_adjust_count"
        private const val KEY_BOOKMARK_COUNT = "bookmark_count"
        private const val KEY_SENTENCE_ANALYSIS_COUNT = "sentence_analysis_count"
        private const val KEY_SCAN_COUNT_TODAY = "scan_count_today"
        private const val KEY_LAST_SCAN_DATE = "last_scan_date"
        private const val KEY_STREAK_DAYS = "streak_days"

        // Milestone keys (persistent, never reset)
        private const val KEY_M_FIRST_CEFR_SCAN = "m_first_cefr_scan"
        private const val KEY_M_FIRST_THRESHOLD_SET = "m_first_threshold_set"
        private const val KEY_M_CEFR_WORDS_50 = "m_cefr_words_50"
        private const val KEY_M_CEFR_WORDS_200 = "m_cefr_words_200"
        private const val KEY_M_CEFR_WORDS_500 = "m_cefr_words_500"
        private const val KEY_M_SCANS_50 = "m_scans_50"
        private const val KEY_M_SCANS_200 = "m_scans_200"
        private const val KEY_M_STREAK_7 = "m_streak_7"
        private const val KEY_M_STREAK_30 = "m_streak_30"
        private const val KEY_M_STREAK_90 = "m_streak_90"

        // Lifetime counters (persistent)
        private const val KEY_TOTAL_CEFR_WORDS = "total_cefr_words"
        private const val KEY_TOTAL_SCANS = "total_scans"
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureToday(context: Context) {
        val prefs = getPrefs(context)
        val today = java.time.LocalDate.now().toString()
        if (prefs.getString(KEY_DATE, null) != today) {
            val yesterday = java.time.LocalDate.now().minusDays(1).toString()
            val lastScanDate = prefs.getString(KEY_LAST_SCAN_DATE, null)
            val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)
            val newStreak = if (lastScanDate == yesterday) {
                currentStreak + 1
            } else {
                // Streak would break — try consuming a streak freeze
                val spendRules = JCoinSpendRules()
                if (currentStreak > 0 && spendRules.consumeStreakFreeze(context)) {
                    currentStreak // preserve streak
                } else {
                    1 // reset
                }
            }

            prefs.edit()
                .putString(KEY_DATE, today)
                .putBoolean(KEY_DAILY_LOGIN_CLAIMED, false)
                .putInt(KEY_DIFFICULT_WORD_COUNT, 0)
                .putInt(KEY_THRESHOLD_ADJUST_COUNT, 0)
                .putInt(KEY_BOOKMARK_COUNT, 0)
                .putInt(KEY_SENTENCE_ANALYSIS_COUNT, 0)
                .putInt(KEY_SCAN_COUNT_TODAY, 0)
                .putInt(KEY_STREAK_DAYS, newStreak)
                .apply()
        }
    }

    // -- Engagement Earns --

    /** Daily login: 2 coins, once per day */
    fun checkDailyLogin(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_DAILY_LOGIN_CLAIMED, false)) return null
        prefs.edit().putBoolean(KEY_DAILY_LOGIN_CLAIMED, true).apply()
        return EarnAction("daily_login", 2)
    }

    /** Scan text: 3 coins per scan */
    fun checkScanText(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SCAN_COUNT_TODAY, 0)
        prefs.edit()
            .putInt(KEY_SCAN_COUNT_TODAY, count + 1)
            .putString(KEY_LAST_SCAN_DATE, java.time.LocalDate.now().toString())
            .apply()
        // Update lifetime total
        val total = prefs.getInt(KEY_TOTAL_SCANS, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_SCANS, total).apply()
        return EarnAction("scan_text", 3)
    }

    /** Difficult word reviewed: 1 coin, 15/day cap */
    fun checkDifficultWordReviewed(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_DIFFICULT_WORD_COUNT, 0)
        if (count >= 15) return null
        prefs.edit().putInt(KEY_DIFFICULT_WORD_COUNT, count + 1).apply()
        // Update lifetime total
        val total = prefs.getInt(KEY_TOTAL_CEFR_WORDS, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_CEFR_WORDS, total).apply()
        return EarnAction("difficult_word_reviewed", 1)
    }

    /** All difficult words cleared: 5 coins per scan */
    fun checkAllDifficultCleared(context: Context): EarnAction {
        return EarnAction("all_difficult_cleared", 5)
    }

    /** CEFR threshold adjusted: 1 coin, 3/day cap */
    fun checkThresholdAdjusted(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_THRESHOLD_ADJUST_COUNT, 0)
        if (count >= 3) return null
        prefs.edit().putInt(KEY_THRESHOLD_ADJUST_COUNT, count + 1).apply()
        return EarnAction("cefr_threshold_adjusted", 1)
    }

    /** Save definition (bookmark): 2 coins, 10/day cap */
    fun checkSaveDefinition(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_BOOKMARK_COUNT, 0)
        if (count >= 10) return null
        prefs.edit().putInt(KEY_BOOKMARK_COUNT, count + 1).apply()
        return EarnAction("save_definition", 2)
    }

    /** Sentence analysis: 3 coins, 5/day cap */
    fun checkSentenceAnalysis(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SENTENCE_ANALYSIS_COUNT, 0)
        if (count >= 5) return null
        prefs.edit().putInt(KEY_SENTENCE_ANALYSIS_COUNT, count + 1).apply()
        return EarnAction("sentence_analysis", 3)
    }

    // -- Milestones (one-time, uncapped) --

    fun checkFirstCefrScan(context: Context): EarnAction? {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_M_FIRST_CEFR_SCAN, false)) return null
        prefs.edit().putBoolean(KEY_M_FIRST_CEFR_SCAN, true).apply()
        return EarnAction("first_cefr_scan", 10)
    }

    fun checkFirstThresholdSet(context: Context): EarnAction? {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_M_FIRST_THRESHOLD_SET, false)) return null
        prefs.edit().putBoolean(KEY_M_FIRST_THRESHOLD_SET, true).apply()
        return EarnAction("first_threshold_set", 5)
    }

    fun checkCefrWordMilestones(context: Context): List<EarnAction> {
        val prefs = getPrefs(context)
        val total = prefs.getInt(KEY_TOTAL_CEFR_WORDS, 0)
        val actions = mutableListOf<EarnAction>()

        if (total >= 50 && !prefs.getBoolean(KEY_M_CEFR_WORDS_50, false)) {
            prefs.edit().putBoolean(KEY_M_CEFR_WORDS_50, true).apply()
            actions.add(EarnAction("cefr_words_50", 25))
        }
        if (total >= 200 && !prefs.getBoolean(KEY_M_CEFR_WORDS_200, false)) {
            prefs.edit().putBoolean(KEY_M_CEFR_WORDS_200, true).apply()
            actions.add(EarnAction("cefr_words_200", 60))
        }
        if (total >= 500 && !prefs.getBoolean(KEY_M_CEFR_WORDS_500, false)) {
            prefs.edit().putBoolean(KEY_M_CEFR_WORDS_500, true).apply()
            actions.add(EarnAction("cefr_words_500", 100))
        }
        return actions
    }

    fun checkScanMilestones(context: Context): List<EarnAction> {
        val prefs = getPrefs(context)
        val total = prefs.getInt(KEY_TOTAL_SCANS, 0)
        val actions = mutableListOf<EarnAction>()

        if (total >= 50 && !prefs.getBoolean(KEY_M_SCANS_50, false)) {
            prefs.edit().putBoolean(KEY_M_SCANS_50, true).apply()
            actions.add(EarnAction("scans_50", 25))
        }
        if (total >= 200 && !prefs.getBoolean(KEY_M_SCANS_200, false)) {
            prefs.edit().putBoolean(KEY_M_SCANS_200, true).apply()
            actions.add(EarnAction("scans_200", 60))
        }
        return actions
    }

    fun checkStreakMilestones(context: Context): List<EarnAction> {
        ensureToday(context)
        val prefs = getPrefs(context)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        val actions = mutableListOf<EarnAction>()

        if (streak >= 7 && !prefs.getBoolean(KEY_M_STREAK_7, false)) {
            prefs.edit().putBoolean(KEY_M_STREAK_7, true).apply()
            actions.add(EarnAction("streak_7_days", 20))
        }
        if (streak >= 30 && !prefs.getBoolean(KEY_M_STREAK_30, false)) {
            prefs.edit().putBoolean(KEY_M_STREAK_30, true).apply()
            actions.add(EarnAction("streak_30_days", 50))
        }
        if (streak >= 90 && !prefs.getBoolean(KEY_M_STREAK_90, false)) {
            prefs.edit().putBoolean(KEY_M_STREAK_90, true).apply()
            actions.add(EarnAction("streak_90_days", 100))
        }
        return actions
    }

    fun getStreakDays(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_STREAK_DAYS, 0)
    }

    fun getTotalScans(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_SCANS, 0)
    }

    fun getTotalCefrWords(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_CEFR_WORDS, 0)
    }
}

data class EarnAction(
    val sourceType: String,
    val coins: Int
)
