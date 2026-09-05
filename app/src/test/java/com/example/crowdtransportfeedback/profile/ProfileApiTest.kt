package com.example.crowdtransportfeedback.profile

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ProfileApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(ProfileApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun pinAndUnpinSendTypedPutRequestsAndAcceptEmptySuccessBodies() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        api.updatePins(listOf("METRO_EXPLORER"))
        server.takeRequest().let { request ->
            assertEquals("PUT", request.method)
            assertEquals("/api/profile/me/pinned-achievements", request.path)
            assertEquals("{\"achievementCodes\":[\"METRO_EXPLORER\"]}", request.body.readUtf8())
        }

        server.enqueue(MockResponse().setResponseCode(200))
        api.updatePins(emptyList())
        server.takeRequest().let { request ->
            assertEquals("PUT", request.method)
            assertEquals("/api/profile/me/pinned-achievements", request.path)
            assertEquals("{\"achievementCodes\":[]}", request.body.readUtf8())
        }
    }
}
