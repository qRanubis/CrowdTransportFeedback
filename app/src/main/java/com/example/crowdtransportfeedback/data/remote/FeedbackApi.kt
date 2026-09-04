package com.example.crowdtransportfeedback.data.remote


import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.Response

interface FeedbackApi {
    @GET("feedback")
    suspend fun getAll(): List<FeedbackDto>

    @GET("feedback/{id}")
    suspend fun getById(@Path("id") id: String): Response<FeedbackDto>

    @POST("feedback")
    suspend fun add(@Body item: FeedbackDto): FeedbackDto

    @DELETE("feedback/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>


}
