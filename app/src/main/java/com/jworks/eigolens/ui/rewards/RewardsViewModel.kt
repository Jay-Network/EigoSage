package com.jworks.eigolens.ui.rewards

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigolens.data.jcoin.DeviceAuthRepository
import com.jworks.eigolens.data.jcoin.JCoinClient
import com.jworks.eigolens.data.jcoin.JCoinEarnRules
import com.jworks.eigolens.data.jcoin.JCoinSpendRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpendItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val isPermanent: Boolean = false,
    val icon: String = ""
)

sealed class PurchaseResult {
    data class Success(val message: String) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val application: Application,
    private val jCoinClient: JCoinClient,
    private val jCoinSpendRules: JCoinSpendRules,
    private val jCoinEarnRules: JCoinEarnRules,
    private val deviceAuthRepository: DeviceAuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RewardsVM"

        val CONSUMABLE_ITEMS = listOf(
            SpendItem("ai_deep_analysis", "AI Deep Analysis", "Full grammar, vocabulary, and comprehension analysis", 8, icon = "\uD83E\uDDE0"),
            SpendItem("cefr_progress_report", "CEFR Progress Report", "Detailed breakdown of your reading level progress", 10, icon = "\uD83D\uDCCA"),
            SpendItem("export_difficult_words", "Export Difficult Words", "Download as CSV or Anki flashcards", 15, icon = "\uD83D\uDCE4"),
            SpendItem("streak_freeze", "Streak Freeze", "Protect your streak for one missed day", 20, icon = "\u2744\uFE0F")
        )

        val THEME_ITEMS = listOf(
            SpendItem("theme_ocean", "Ocean Theme", "Cool blue tones inspired by the sea", 30, isPermanent = true, icon = "\uD83C\uDF0A"),
            SpendItem("theme_forest", "Forest Theme", "Natural green tones of the woods", 30, isPermanent = true, icon = "\uD83C\uDF32"),
            SpendItem("theme_sunset", "Sunset Theme", "Warm gradient from orange to purple", 45, isPermanent = true, icon = "\uD83C\uDF05"),
            SpendItem("theme_galaxy", "Galaxy Theme", "Deep space purples and starlight", 60, isPermanent = true, icon = "\uD83C\uDF0C")
        )
    }

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _tier = MutableStateFlow("bronze")
    val tier: StateFlow<String> = _tier.asStateFlow()

    private val _freeAiRemaining = MutableStateFlow(0)
    val freeAiRemaining: StateFlow<Int> = _freeAiRemaining.asStateFlow()

    private val _streakFreezesOwned = MutableStateFlow(0)
    val streakFreezesOwned: StateFlow<Int> = _streakFreezesOwned.asStateFlow()

    private val _unlockedThemes = MutableStateFlow<Set<String>>(emptySet())
    val unlockedThemes: StateFlow<Set<String>> = _unlockedThemes.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _totalScans = MutableStateFlow(0)
    val totalScans: StateFlow<Int> = _totalScans.asStateFlow()

    private val _totalCefrWords = MutableStateFlow(0)
    val totalCefrWords: StateFlow<Int> = _totalCefrWords.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _purchaseResult = MutableSharedFlow<PurchaseResult>()
    val purchaseResult = _purchaseResult.asSharedFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        val ctx = application.applicationContext
        _freeAiRemaining.value = jCoinSpendRules.freeAiAnalysisRemaining(ctx)
        _streakFreezesOwned.value = jCoinSpendRules.getStreakFreezesOwned(ctx)
        _unlockedThemes.value = jCoinSpendRules.getUnlockedThemes(ctx)
        _streakDays.value = jCoinEarnRules.getStreakDays(ctx)
        _totalScans.value = jCoinEarnRules.getTotalScans(ctx)
        _totalCefrWords.value = jCoinEarnRules.getTotalCefrWords(ctx)

        viewModelScope.launch {
            val token = deviceAuthRepository.getAccessToken()
            jCoinClient.getBalance(token)
                .onSuccess { balance ->
                    _coinBalance.value = balance.balance
                    _tier.value = balance.tier
                }
                .onFailure { Log.d(TAG, "Balance fetch failed: ${it.message}") }
        }
    }

    fun purchaseItem(item: SpendItem) {
        if (_isPurchasing.value) return
        _isPurchasing.value = true

        viewModelScope.launch {
            try {
                val balance = _coinBalance.value
                if (balance < item.cost) {
                    _purchaseResult.emit(PurchaseResult.Error("Not enough coins (need ${item.cost}, have $balance)"))
                    return@launch
                }

                // For permanent items, check if already owned
                if (item.isPermanent && item.id.startsWith("theme_")) {
                    val themeId = item.id.removePrefix("theme_")
                    if (jCoinSpendRules.isThemeUnlocked(application.applicationContext, themeId)) {
                        _purchaseResult.emit(PurchaseResult.Error("Already unlocked!"))
                        return@launch
                    }
                }

                val token = deviceAuthRepository.getAccessToken()
                jCoinClient.spend(token, item.id, item.cost)
                    .onSuccess { response ->
                        _coinBalance.value = response.newBalance.toInt()

                        // Apply item-specific effects
                        val ctx = application.applicationContext
                        when (item.id) {
                            "streak_freeze" -> {
                                jCoinSpendRules.purchaseStreakFreeze(ctx)
                                _streakFreezesOwned.value = jCoinSpendRules.getStreakFreezesOwned(ctx)
                            }
                            else -> {
                                if (item.id.startsWith("theme_")) {
                                    val themeId = item.id.removePrefix("theme_")
                                    jCoinSpendRules.unlockTheme(ctx, themeId)
                                    _unlockedThemes.value = jCoinSpendRules.getUnlockedThemes(ctx)
                                }
                            }
                        }
                        _purchaseResult.emit(PurchaseResult.Success("Purchased ${item.name}!"))
                    }
                    .onFailure { e ->
                        _purchaseResult.emit(PurchaseResult.Error("Purchase failed: ${e.message}"))
                    }
            } finally {
                _isPurchasing.value = false
            }
        }
    }
}
