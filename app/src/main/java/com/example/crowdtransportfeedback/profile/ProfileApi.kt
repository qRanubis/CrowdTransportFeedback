package com.example.crowdtransportfeedback.profile
import retrofit2.http.*
data class LevelDto(val level:Int,val title:String,val levelStartXp:Long,val xpIntoLevel:Long,val xpNeededForNextLevel:Long?,val nextLevelThreshold:Int?,val maxLevel:Boolean)
data class BadgeDto(val code:String,val title:String,val description:String,val category:String,val unlocked:Boolean,val unlockedAt:String?,val currentProgress:Long,val targetProgress:Int,val pinned:Boolean,val pinOrder:Int?=null)
data class ProfileDto(val username:String,val avatarKey:String,val totalXp:Long,val level:LevelDto,val contributionCount:Long,val differentLineCount:Long,val transportTypeCount:Long,val unlockedAchievementCount:Long,val achievementTotal:Int=28,val allTimeXpRank:Long?=null,val pinnedAchievements:List<BadgeDto> = emptyList(),val contributionBreakdown:Map<String,Long> = emptyMap())
data class LeaderboardEntryDto(val rank:Long?,val username:String,val avatarKey:String,val level:Int,val metricValue:Long,val totalXp:Long,val currentUser:Boolean)
data class LeaderboardDto(val top:List<LeaderboardEntryDto>,val currentUser:LeaderboardEntryDto)
interface ProfileApi {
 @GET("api/profile/me") suspend fun me():ProfileDto
 @GET("api/profile/{username}") suspend fun profile(@Path("username") username:String):ProfileDto
 @GET("api/profile/me/achievements") suspend fun achievements():List<BadgeDto>
 @PATCH("api/profile/me/avatar") suspend fun avatar(@Body body:Map<String,String>)
 @PUT("api/profile/me/pinned-achievements") suspend fun pins(@Body body:Map<String,List<String>>)
 @GET("api/leaderboard") suspend fun leaderboard(@Query("metric") metric:String,@Query("period") period:String):LeaderboardDto
}
