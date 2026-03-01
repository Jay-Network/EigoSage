package com.jworks.eigosage.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val email: String? = null
)

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun getCurrentUserId(): String?
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signOut()
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun refreshAuthState()
}
