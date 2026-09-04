package com.example.crowdtransportfeedback.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Route

class AccessTokenInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response { val request = chain.request(); val token = session.accessToken(); return chain.proceed(if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build()) }
}
class AccessTokenAuthenticator(private val session: SessionManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): okhttp3.Request? {
        if (responseCount(response) >= 2) return null
        val observed = response.request.header("Authorization")?.removePrefix("Bearer ")
        val refreshed = runBlocking { session.refresh(observed) } ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $refreshed").build()
    }
    private fun responseCount(response: Response): Int { var count=1; var prior=response.priorResponse; while(prior!=null){count++;prior=prior.priorResponse};return count }
}
