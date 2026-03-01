package com.jworks.eigosage.ui.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jworks.eigosage.BuildConfig
import com.jworks.eigosage.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isSignUpMode: Boolean = false,
    val googleSignInAvailable: Boolean = BuildConfig.GCP_WEB_CLIENT_ID.isNotBlank()
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthVM"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuth()
        restoreSession()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                _uiState.value = _uiState.value.copy(isLoggedIn = state.isLoggedIn)
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            authRepository.refreshAuthState()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signIn(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sanitizeError(e, "Sign in failed")
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signUp(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = sanitizeError(e, "Sign up failed")
                    )
                }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        val webClientId = BuildConfig.GCP_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Google Sign-In not configured")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                authRepository.signInWithGoogle(idToken)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = sanitizeError(e, "Google sign in failed")
                        )
                    }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.d(TAG, "Google sign in cancelled")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Google sign in error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google Sign-In failed. Please try email sign in."
                )
            }
        }
    }

    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun sanitizeError(e: Throwable, fallback: String): String {
        val msg = e.message ?: return fallback
        return when {
            "Invalid login credentials" in msg -> "Invalid email or password"
            "Email not confirmed" in msg -> "Please confirm your email first"
            "User already registered" in msg -> "An account with this email already exists"
            "Auth not configured" in msg -> "Authentication is not available"
            "rate limit" in msg.lowercase() -> "Too many attempts. Please try again later."
            else -> fallback
        }
    }
}
