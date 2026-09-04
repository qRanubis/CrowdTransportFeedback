package com.example.crowdtransportfeedback.auth

data class Credentials(val email: String, val password: String)
data class RegisterRequest(val email: String, val username: String, val password: String)
data class RefreshRequest(val refreshToken: String)

enum class UserRole { USER, ADMIN }

data class AuthUser(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser
)

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser
)

sealed interface SessionState {
    data object Loading : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val user: AuthUser, val offline: Boolean = false) : SessionState
}

private val emailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
private val usernameRegex = Regex("""^[a-z0-9]{3,20}$""")

fun normalizeEmail(value: String): String = value.trim().lowercase()

fun isValidEmail(value: String): Boolean = emailRegex.matches(normalizeEmail(value))

fun isValidUsername(value: String): Boolean = usernameRegex.matches(value)

fun registrationPasswordError(password: String): String? = when {
    password.length < 8 -> "Password must have at least 8 characters."
    password.length > 128 -> "Password must have at most 128 characters."
    password.none(Char::isLowerCase) -> "Password must include a lowercase letter."
    password.none(Char::isUpperCase) -> "Password must include an uppercase letter."
    password.none(Char::isDigit) -> "Password must include a digit."
    password.none { !it.isLetterOrDigit() && !it.isWhitespace() } ->
        "Password must include a symbol."
    else -> null
}

fun canDeleteFeedback(
    role: UserRole,
    currentUserId: String,
    ownerUserId: String?
): Boolean = role == UserRole.ADMIN || ownerUserId == currentUserId
