package com.example.crowdtransportfeedback.auth

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SessionAndNetworkingTest {
    @Test
    fun `only admin role can expose delete`() {
        assertFalse(canDeleteFeedback(UserRole.USER))
        assertTrue(canDeleteFeedback(UserRole.ADMIN))
    }

    @Test
    fun `restore keeps a persisted user authenticated`() {
        val store = MemoryTokenStore(session())
        val manager = SessionManager(TestAuthApi(), store)

        manager.restore()

        assertEquals(SessionState.Authenticated(session().user), manager.state.value)
    }

    @Test
    fun `restore without tokens is unauthenticated`() {
        val manager = SessionManager(TestAuthApi(), MemoryTokenStore())

        manager.restore()

        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun `interceptor attaches bearer token`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()
        try {
            val manager = SessionManager(TestAuthApi(), MemoryTokenStore(session()))
            val client = OkHttpClient.Builder()
                .addInterceptor(AccessTokenInterceptor(manager))
                .build()

            client.newCall(okhttp3.Request.Builder().url(server.url("/")).build()).execute().close()

            assertEquals("Bearer access", server.takeRequest().getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `online logout revokes remotely and clears local session`() = runBlocking {
        val api = TestAuthApi()
        val store = MemoryTokenStore(session())
        val manager = SessionManager(api, store)
        manager.restore()

        manager.logout()

        assertEquals(1, api.logoutCalls)
        assertNull(store.read())
        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun `offline logout still clears local session`() = runBlocking {
        val api = TestAuthApi(logoutMode = LogoutMode.OFFLINE)
        val store = MemoryTokenStore(session())
        val manager = SessionManager(api, store)
        manager.restore()

        manager.logout()

        assertEquals(1, api.logoutCalls)
        assertNull(store.read())
        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun `server failure during logout still clears local session`() = runBlocking {
        val api = TestAuthApi(logoutMode = LogoutMode.SERVER_ERROR)
        val store = MemoryTokenStore(session())
        val manager = SessionManager(api, store)
        manager.restore()

        manager.logout()

        assertEquals(1, api.logoutCalls)
        assertNull(store.read())
        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }

    @Test
    fun `login and register both signal immediate synchronization`() = runBlocking {
        val api = TestAuthApi()
        var scheduled = 0
        val repository = AuthRepository(api, MemoryTokenStore()) { scheduled++ }

        repository.login("user@example.com", "password123")
        repository.register("user@example.com", "password123")

        assertEquals(2, scheduled)
    }

    @Test
    fun `temporary refresh network failure preserves local session`() = runBlocking {
        val api = TestAuthApi(refreshOffline = true)
        val store = MemoryTokenStore(session())
        val manager = SessionManager(api, store)
        manager.restore()

        val refreshed = manager.refresh("access")

        assertNull(refreshed)
        assertTrue(manager.hasTemporaryRefreshFailure())
        assertEquals(session(), store.read())
    }

    private fun session() = StoredSession(
        "access",
        "refresh",
        AuthUser("id", "user@example.com", UserRole.USER)
    )
}

private class MemoryTokenStore(private var value: StoredSession? = null) : TokenStore {
    override fun read() = value
    override fun save(session: StoredSession) {
        value = session
    }
    override fun clear() {
        value = null
    }
}

private enum class LogoutMode { OK, OFFLINE, SERVER_ERROR }

private class TestAuthApi(
    private val logoutMode: LogoutMode = LogoutMode.OK,
    private val refreshOffline: Boolean = false
) : AuthApi {
    var logoutCalls = 0

    private fun authResponse() = AuthResponse(
        accessToken = "new-access",
        refreshToken = "new-refresh",
        user = AuthUser("id", "user@example.com", UserRole.USER)
    )

    override suspend fun register(credentials: Credentials): AuthResponse = authResponse()

    override suspend fun login(credentials: Credentials): AuthResponse = authResponse()

    override suspend fun refresh(request: RefreshRequest): Response<AuthResponse> {
        if (refreshOffline) throw IOException("offline")
        return Response.success(authResponse())
    }

    override suspend fun logout(request: RefreshRequest): Response<Unit> {
        logoutCalls++
        return when (logoutMode) {
            LogoutMode.OK -> Response.success(Unit)
            LogoutMode.OFFLINE -> throw IOException("offline")
            LogoutMode.SERVER_ERROR -> Response.error(500, "server error".toResponseBody())
        }
    }
}
