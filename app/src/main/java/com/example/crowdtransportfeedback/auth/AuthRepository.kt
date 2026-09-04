package com.example.crowdtransportfeedback.auth

class AuthRepository(
    private val api: AuthApi,
    private val tokens: TokenStore,
    private val onAuthenticated: () -> Unit = {}
) {
    suspend fun login(email: String, password: String) =
        persist(api.login(Credentials(email.trim(), password)))

    suspend fun register(email: String, password: String) =
        persist(api.register(Credentials(email.trim(), password)))

    private fun persist(response: AuthResponse): StoredSession {
        val session = StoredSession(response.accessToken, response.refreshToken, response.user)
        tokens.save(session)
        onAuthenticated()
        return session
    }
}
