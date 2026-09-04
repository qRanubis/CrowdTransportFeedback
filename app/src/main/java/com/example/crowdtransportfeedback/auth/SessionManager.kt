package com.example.crowdtransportfeedback.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class SessionManager(private val api: AuthApi, private val tokens: TokenStore) {
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state

    fun restore() {
        _state.value = tokens.read()?.let { SessionState.Authenticated(it.user) }
            ?: SessionState.Unauthenticated
    }

    fun authenticated(session: StoredSession) {
        _state.value = SessionState.Authenticated(session.user)
    }

    fun accessToken(): String? = tokens.read()?.accessToken
    fun user(): AuthUser? = tokens.read()?.user

    fun hasTemporaryRefreshFailure(): Boolean =
        (state.value as? SessionState.Authenticated)?.offline == true

    suspend fun refresh(observedAccessToken: String?): String? = refreshMutex.withLock {
        val current = tokens.read() ?: return null
        if (observedAccessToken != null && current.accessToken != observedAccessToken) {
            return current.accessToken
        }

        val response = try {
            api.refresh(RefreshRequest(current.refreshToken))
        } catch (_: IOException) {
            _state.value = SessionState.Authenticated(current.user, offline = true)
            return null
        }

        if (response.isSuccessful) {
            val body = response.body() ?: return null
            tokens.save(StoredSession(body.accessToken, body.refreshToken, body.user))
            _state.value = SessionState.Authenticated(body.user)
            body.accessToken
        } else if (response.code() == 401 || response.code() == 403) {
            clear()
            null
        } else {
            _state.value = SessionState.Authenticated(current.user, offline = true)
            null
        }
    }

    suspend fun logout() {
        val refreshToken = tokens.read()?.refreshToken
        try {
            if (refreshToken != null) {
                runCatching { api.logout(RefreshRequest(refreshToken)) }
            }
        } finally {
            clear()
        }
    }

    fun clear() {
        tokens.clear()
        _state.value = SessionState.Unauthenticated
    }
}
