package com.example.crowdtransportfeedback.auth

class AuthRepository(private val api: AuthApi, private val tokens: TokenStore) {
    suspend fun login(email: String, password: String) = persist(api.login(Credentials(email.trim(), password)))
    suspend fun register(email: String, password: String) = persist(api.register(Credentials(email.trim(), password)))
    suspend fun logout() { val refresh = tokens.read()?.refreshToken; try { if (refresh != null) api.logout(RefreshRequest(refresh)) } finally { tokens.clear() } }
    private fun persist(response: AuthResponse): StoredSession = StoredSession(response.accessToken, response.refreshToken, response.user).also(tokens::save)
}
