package com.example.crowdtransportfeedback.data.remote


import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.Response

interface FeedbackApi {
    data class ReportRequest(val reason: String, val details: String?)
    data class MyReport(val reported: Boolean, val status: String?)
    @GET("api/feedback")
    suspend fun getAll(): List<FeedbackDto>

    @GET("api/feedback/{id}")
    suspend fun getById(@Path("id") id: String): Response<FeedbackDto>

    @POST("api/feedback")
    suspend fun add(@Body item: FeedbackDto): FeedbackDto

    @DELETE("api/feedback/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @GET("api/feedback/{id}/reports/me")
    suspend fun myReport(@Path("id") id: String): MyReport

    @POST("api/feedback/{id}/reports")
    suspend fun report(@Path("id") id: String, @Body request: ReportRequest): Response<Unit>


}
