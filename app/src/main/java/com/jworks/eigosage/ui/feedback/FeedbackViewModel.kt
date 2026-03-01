package com.jworks.eigosage.ui.feedback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigosage.domain.models.FeedbackCategory
import com.jworks.eigosage.domain.models.FeedbackWithHistory
import com.jworks.eigosage.domain.models.SubmitFeedbackResult
import com.jworks.eigosage.domain.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.jworks.eigosage.service.FeedbackFCMService
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackList: List<FeedbackWithHistory> = emptyList(),
    val isDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedCategory: FeedbackCategory = FeedbackCategory.OTHER,
    val feedbackText: String = "",
    val userEmail: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val isLoadingHistory: Boolean = false
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val APP_ID = "eigosage"
        private const val POLL_INTERVAL_MS = 15_000L
        private const val PREFS_NAME = "eigosage_prefs"
        private const val KEY_USER_EMAIL = "feedback_user_email"
    }

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var lastFeedbackId: Long? = null

    init {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        _uiState.value = _uiState.value.copy(userEmail = savedEmail)
    }

    fun openDialog() {
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            feedbackText = "",
            selectedCategory = FeedbackCategory.OTHER,
            error = null,
            successMessage = null
        )

        if (_uiState.value.userEmail.isNotBlank()) {
            loadFeedbackHistory()
            startPolling()
        }
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(isDialogOpen = false)
        stopPolling()
    }

    fun selectCategory(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateFeedbackText(text: String) {
        _uiState.value = _uiState.value.copy(feedbackText = text)
    }

    fun updateUserEmail(email: String) {
        _uiState.value = _uiState.value.copy(userEmail = email)
    }

    fun submitFeedback() {
        val email = _uiState.value.userEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }

        val text = _uiState.value.feedbackText.trim()
        if (text.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please provide at least 10 characters")
            return
        }
        if (text.length > 1000) {
            _uiState.value = _uiState.value.copy(error = "Maximum 1000 characters allowed")
            return
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_EMAIL, email)
            .apply()

        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val deviceInfo = mapOf(
                "os" to "Android",
                "osVersion" to android.os.Build.VERSION.RELEASE,
                "device" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER
            )

            when (val result = feedbackRepository.submitFeedback(
                email = email,
                appId = APP_ID,
                category = _uiState.value.selectedCategory,
                feedbackText = text,
                deviceInfo = deviceInfo
            )) {
                is SubmitFeedbackResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Thank you for your feedback! We'll keep you updated on progress.",
                        feedbackText = "",
                        selectedCategory = FeedbackCategory.OTHER
                    )
                    loadFeedbackHistory()
                    startPolling()
                }
                is SubmitFeedbackResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                is SubmitFeedbackResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun loadFeedbackHistory() {
        val email = _uiState.value.userEmail.trim()
        if (email.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoadingHistory = true)

        viewModelScope.launch {
            try {
                val feedback = feedbackRepository.getFeedbackUpdates(
                    email = email,
                    appId = APP_ID,
                    sinceId = null
                )

                lastFeedbackId = feedback.maxOfOrNull { it.feedback.id }

                _uiState.value = _uiState.value.copy(
                    feedbackList = feedback.sortedByDescending { it.feedback.createdAt },
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Failed to load feedback history: ${e.message}"
                )
            }
        }
    }

    private fun startPolling() {
        stopPolling()

        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val email = _uiState.value.userEmail.trim()
                if (email.isBlank()) continue

                try {
                    val newFeedback = feedbackRepository.getFeedbackUpdates(
                        email = email,
                        appId = APP_ID,
                        sinceId = lastFeedbackId
                    )

                    if (newFeedback.isNotEmpty()) {
                        val updatedList = (_uiState.value.feedbackList + newFeedback)
                            .distinctBy { it.feedback.id }
                            .sortedByDescending { it.feedback.createdAt }

                        lastFeedbackId = updatedList.maxOfOrNull { it.feedback.id }

                        _uiState.value = _uiState.value.copy(feedbackList = updatedList)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun registerFcmToken(token: String) {
        if (!FeedbackFCMService.isFirebaseAvailable()) return

        val email = _uiState.value.userEmail.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            try {
                val deviceInfo = mapOf(
                    "os" to "Android",
                    "osVersion" to android.os.Build.VERSION.RELEASE,
                    "device" to android.os.Build.MODEL
                )

                feedbackRepository.registerFcmToken(
                    email = email,
                    appId = APP_ID,
                    fcmToken = token,
                    deviceInfo = deviceInfo
                )
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
