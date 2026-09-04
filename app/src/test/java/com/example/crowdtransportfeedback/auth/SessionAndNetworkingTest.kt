package com.example.crowdtransportfeedback.auth

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class SessionAndNetworkingTest {
    @Test fun `only admin role can expose delete`() { assertFalse(canDeleteFeedback(UserRole.USER)); assertTrue(canDeleteFeedback(UserRole.ADMIN)) }
    @Test fun `restore keeps a persisted user authenticated`() {
        val store = MemoryTokenStore(session())
        val manager = SessionManager(NoopAuthApi, store)
        manager.restore()
        assertEquals(SessionState.Authenticated(session().user), manager.state.value)
    }
    @Test fun `restore without tokens is unauthenticated`() {
        val manager = SessionManager(NoopAuthApi, MemoryTokenStore())
        manager.restore()
        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }
    @Test fun `interceptor attaches bearer token`() {
        val server=MockWebServer(); server.enqueue(MockResponse().setResponseCode(200)); server.start()
        val manager=SessionManager(NoopAuthApi,MemoryTokenStore(session())); val client=OkHttpClient.Builder().addInterceptor(AccessTokenInterceptor(manager)).build()
        client.newCall(okhttp3.Request.Builder().url(server.url("/")).build()).execute().close()
        assertEquals("Bearer access",server.takeRequest().getHeader("Authorization"));server.shutdown()
    }
    private fun session()=StoredSession("access","refresh",AuthUser("id","user@example.com",UserRole.USER))
}
private class MemoryTokenStore(private var value:StoredSession?=null):TokenStore{override fun read()=value;override fun save(session:StoredSession){value=session};override fun clear(){value=null}}
private object NoopAuthApi:AuthApi{
 override suspend fun register(credentials:Credentials)=error("unused");override suspend fun login(credentials:Credentials)=error("unused")
 override suspend fun refresh(request:RefreshRequest):Response<AuthResponse> = error("unused");override suspend fun logout(request:RefreshRequest):Response<Unit> = error("unused")
}
