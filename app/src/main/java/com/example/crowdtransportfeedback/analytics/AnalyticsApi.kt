package com.example.crowdtransportfeedback.analytics
import retrofit2.http.GET; import retrofit2.http.Query
interface AnalyticsApi {
 @GET("api/analytics/heatmap") suspend fun heatmap(@Query("metric") metric:String,@Query("transportType") transportType:String?,@Query("line") line:String?,@Query("window") window:String):List<AnalyticsCellDto>
 @GET("api/analytics/area") suspend fun area(@Query("cellId") cellId:String,@Query("metric") metric:String,@Query("transportType") transportType:String?,@Query("line") line:String?,@Query("window") window:String):AreaDetailsDto
}
