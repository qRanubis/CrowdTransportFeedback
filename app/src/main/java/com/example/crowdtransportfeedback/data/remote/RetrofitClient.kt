package com.example.crowdtransportfeedback.data.remote

import com.example.crowdtransportfeedback.auth.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient(tokenStore: TokenStore) {
    companion object { const val BASE_URL = "http://10.0.2.2:8080/" }
    private val publicRetrofit = Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    val authApi: AuthApi = publicRetrofit.create(AuthApi::class.java)
    val sessionManager = SessionManager(authApi, tokenStore)
    private val protectedClient = OkHttpClient.Builder().addInterceptor(AccessTokenInterceptor(sessionManager)).authenticator(AccessTokenAuthenticator(sessionManager)).build()
    val feedbackApi: FeedbackApi = Retrofit.Builder().baseUrl(BASE_URL).client(protectedClient).addConverterFactory(GsonConverterFactory.create()).build().create(FeedbackApi::class.java)
}
