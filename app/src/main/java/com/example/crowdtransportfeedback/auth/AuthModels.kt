package com.example.crowdtransportfeedback.auth

data class Credentials(val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)
enum class UserRole { USER, ADMIN }
fun canDeleteFeedback(role: UserRole): Boolean = role == UserRole.ADMIN
data class AuthUser(val id: String, val email: String, val role: UserRole)
data class AuthResponse(val accessToken: String, val refreshToken: String, val user: AuthUser)
data class StoredSession(val accessToken: String, val refreshToken: String, val user: AuthUser)

sealed interface SessionState {
    data object Loading : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val user: AuthUser, val offline: Boolean = false) : SessionState
}
