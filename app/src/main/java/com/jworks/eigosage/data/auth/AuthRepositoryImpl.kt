package com.jworks.eigosage.data.auth

import android.util.Log
import com.jworks.eigosage.domain.auth.AuthRepository
import com.jworks.eigosage.domain.auth.AuthState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    companion object {
        private const val TAG = "AuthRepo"
    }

    private val _authState = MutableStateFlow(AuthState())

    override fun observeAuthState(): Flow<AuthState> = _authState

    override suspend fun getCurrentUserId(): String? {
        if (!SupabaseClientFactory.isInitialized()) return null
        return try {
            val client = SupabaseClientFactory.getInstance()
            client.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get user ID", e)
            null
        }
    }

    override suspend fun signIn(email: String, password: String): Result<String> {
        if (!SupabaseClientFactory.isInitialized()) {
            return Result.failure(IllegalStateException("Auth not configured"))
        }
        return try {
            val client = SupabaseClientFactory.getInstance()
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentSessionOrNull()?.user?.id
                ?: return Result.failure(IllegalStateException("No session after sign in"))
            _authState.value = AuthState(isLoggedIn = true, userId = userId, email = email)
            Log.i(TAG, "Signed in: $email")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        if (!SupabaseClientFactory.isInitialized()) {
            return Result.failure(IllegalStateException("Auth not configured"))
        }
        return try {
            val client = SupabaseClientFactory.getInstance()
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentSessionOrNull()?.user?.id
                ?: return Result.failure(IllegalStateException("No session after sign up"))
            _authState.value = AuthState(isLoggedIn = true, userId = userId, email = email)
            Log.i(TAG, "Signed up: $email")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        if (!SupabaseClientFactory.isInitialized()) return
        try {
            val client = SupabaseClientFactory.getInstance()
            client.auth.signOut()
            Log.i(TAG, "Signed out")
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error", e)
        }
        _authState.value = AuthState()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        if (!SupabaseClientFactory.isInitialized()) {
            return Result.failure(IllegalStateException("Auth not configured"))
        }
        return try {
            val client = SupabaseClientFactory.getInstance()
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            val session = client.auth.currentSessionOrNull()
            val userId = session?.user?.id
                ?: return Result.failure(IllegalStateException("No session after Google sign in"))
            val email = session.user?.email
            _authState.value = AuthState(isLoggedIn = true, userId = userId, email = email)
            Log.i(TAG, "Google sign in: $email")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshAuthState() {
        if (!SupabaseClientFactory.isInitialized()) return
        try {
            val client = SupabaseClientFactory.getInstance()
            val session = client.auth.currentSessionOrNull()
            if (session != null) {
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userId = session.user?.id,
                    email = session.user?.email
                )
                Log.i(TAG, "Session restored for ${session.user?.email}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore session", e)
        }
    }
}
