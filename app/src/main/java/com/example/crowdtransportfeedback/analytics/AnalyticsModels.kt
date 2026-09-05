package com.example.crowdtransportfeedback.analytics

data class AnalyticsCellDto(val cellId:String,val centerLatitude:Double,val centerLongitude:Double,val score:Double,val confidence:String,val feedbackCount:Int,val uniqueContributorCount:Int,val latestCreatedAt:Long,val trustScore:Double,val punctualityScore:Double,val cleanlinessScore:Double,val crowdingComfortScore:Double)
data class AnalyticsPreviewDto(val feedbackId:String,val createdByUsername:String,val transportType:String,val line:String,val overallRating:Double,val createdAt:Long)
data class AreaDetailsDto(val cellId:String,val centerLatitude:Double,val centerLongitude:Double,val score:Double,val confidence:String,val feedbackCount:Int,val uniqueContributorCount:Int,val latestCreatedAt:Long,val trustScore:Double,val punctualityScore:Double,val cleanlinessScore:Double,val crowdingComfortScore:Double,val latestFeedbacks:List<AnalyticsPreviewDto>)
enum class AnalyticsMetric { TRUST, CROWDING, PUNCTUALITY, CLEANLINESS }
enum class AnalyticsWindow(val query:String,val label:String){ H24("24H","24 hours"),D7("7D","7 days"),D30("30D","30 days"),ALL("ALL","All time") }
enum class MapMode { FEEDBACK, HEATMAP }
data class AnalyticsFilter(val metric:AnalyticsMetric=AnalyticsMetric.TRUST,val window:AnalyticsWindow=AnalyticsWindow.D30,val transportType:String?=null,val line:String?=null){fun withTransport(value:String?)=copy(transportType=value,line=null)}
fun normalizedHeatWeight(score:Double)=((score.coerceIn(1.0,5.0)-1.0)/4.0)
fun AnalyticsMetric.legend()=when(this){AnalyticsMetric.TRUST->"Trust: Low ←→ High";AnalyticsMetric.CROWDING->"Crowding: Crowded ←→ Comfortable";AnalyticsMetric.PUNCTUALITY->"Punctuality: Poor ←→ Punctual";AnalyticsMetric.CLEANLINESS->"Cleanliness: Poor ←→ Clean"}
fun heatmapErrorForMode(mode:MapMode,error:String?)=error.takeIf { mode==MapMode.HEATMAP }
fun shouldShowHeatmapEmpty(mode:MapMode,loading:Boolean,error:String?,cellCount:Int)=mode==MapMode.HEATMAP&&!loading&&error==null&&cellCount==0
