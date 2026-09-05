package com.example.crowdtransportfeedback.admin
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import com.example.crowdtransportfeedback.data.remote.FeedbackDto

data class AdminOverview(val totalUsers:Long=0,val totalFeedbacks:Long=0,val feedbackLast24h:Long=0,val feedbackLast7d:Long=0,val feedbackLast30d:Long=0,val activeContributors30d:Long=0,val pendingReports:Long=0,val reportedFeedbackAwaitingReview:Long=0,val feedbackByTransportType:Map<String,Long> = emptyMap(),val topLines:List<TopLine> = emptyList())
data class TopLine(val transportType:String,val line:String,val count:Long)
data class AdminPage<T>(val content:List<T> = emptyList(),val page:Int=0,val size:Int=20,val totalElements:Long=0,val totalPages:Int=0)
data class QueueItem(val feedbackId:String,val authorUsername:String,val transportType:String,val line:String,val score:Double,val reportCount:Long,val lastReportedAt:String,val reasonCounts:Map<String,Long>)
data class AdminFeedback(val feedbackId:String,val createdAt:Long,val username:String,val transportType:String,val line:String,val score:Double,val comment:String?)
data class AdminUser(val id:String,val username:String,val role:String,val joinedAt:String,val feedbackCount:Long,val totalXp:Long,val level:Int,val verifiedReportCount:Long)
data class ReportingSummary(val feedbackCount:Long=0,val uniqueContributors:Long=0,val averageOverall:Double?=null,val averagePunctuality:Double?=null,val averageCleanliness:Double?=null,val averageCrowding:Double?=null,val mostActiveTransportType:String?=null,val mostActiveLine:String?=null)
data class PendingReport(val id:String,val reporterUsername:String,val reason:String,val details:String?,val createdAt:String)
data class ModerationDetail(val feedback:FeedbackDto,val reports:List<PendingReport>)
data class ResolveRequest(val action:String,val note:String?=null)
interface AdminApi {
 @GET("api/admin/overview") suspend fun overview():AdminOverview
 @GET("api/admin/moderation/reports") suspend fun reports(@Query("page")page:Int=0,@Query("size")size:Int=20):AdminPage<QueueItem>
 @GET("api/admin/moderation/feedback/{id}") suspend fun detail(@Path("id")id:String):ModerationDetail
 @POST("api/admin/moderation/feedback/{id}/resolve") suspend fun resolve(@Path("id")id:String,@Body request:ResolveRequest):Response<Unit>
 @GET("api/admin/feedback") suspend fun feedback(@Query("transportType")transportType:String?=null,@Query("line")line:String?=null,@Query("window")window:String="ALL",@Query("username")username:String?=null,@Query("page")page:Int=0,@Query("size")size:Int=20):AdminPage<AdminFeedback>
 @GET("api/admin/users") suspend fun users(@Query("query")query:String?=null,@Query("page")page:Int=0,@Query("size")size:Int=20):AdminPage<AdminUser>
 @GET("api/admin/reporting/summary") suspend fun summary(@Query("window")window:String="ALL",@Query("transportType")transportType:String?=null,@Query("line")line:String?=null):ReportingSummary
 @GET("api/admin/reporting/feedback.csv") suspend fun csv(@Query("window")window:String="ALL",@Query("transportType")transportType:String?=null,@Query("line")line:String?=null):ResponseBody
}
