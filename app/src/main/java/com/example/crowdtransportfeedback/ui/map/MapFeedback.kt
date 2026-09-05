package com.example.crowdtransportfeedback.ui.map

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.domain.TransportType

data class MapFeedbackMarker(
    val localId: Long,
    val latitude: Double,
    val longitude: Double,
    val transportType: TransportType,
    val line: String,
    val overallRating: Double,
    val publicUsername: String,
    val createdAt: Long
)

data class MapFeedbackGroup(val cellId:String,val latitude:Double,val longitude:Double,val feedback:List<MapFeedbackMarker>)

fun feedbackGroups(markers:List<MapFeedbackMarker>):List<MapFeedbackGroup> = markers.groupBy { geoCellId(it.latitude,it.longitude) }.map { (id, rows) ->
    val center=geoCellCenter(id); MapFeedbackGroup(id,center.first,center.second,rows.sortedByDescending { it.createdAt })
}

fun geoCellId(latitude:Double,longitude:Double):String { val r=6378137.0;val x=r*Math.toRadians(longitude);val y=r*kotlin.math.ln(kotlin.math.tan(Math.PI/4+Math.toRadians(latitude)/2));return "${kotlin.math.floor(x/250).toLong()}:${kotlin.math.floor(y/250).toLong()}" }
fun geoCellCenter(id:String):Pair<Double,Double>{val (x,y)=id.split(":").map(String::toLong);val r=6378137.0;val mx=(x+.5)*250;val my=(y+.5)*250;return Math.toDegrees(2*kotlin.math.atan(kotlin.math.exp(my/r))-Math.PI/2) to Math.toDegrees(mx/r)}
fun <T> newestPage(rows:List<T>,createdAt:(T)->Long,page:Int)=rows.sortedByDescending(createdAt).take(page*20)

enum class MapFilter(val label: String, val transportType: TransportType?) {
    ALL("All", null),
    BUS("Bus", TransportType.BUS),
    METRO("Metro", TransportType.METRO),
    TRAM("Tram", TransportType.TRAM),
    TROLLEYBUS("Trolleybus", TransportType.TROLLEYBUS),
    NIGHT_BUS("Night bus", TransportType.NIGHT_BUS)
}

fun FeedbackEntity.toMapFeedbackMarker(): MapFeedbackMarker? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val type = transportType ?: return null
    if (syncState != SyncState.SYNCED || !lat.isFinite() || !lng.isFinite() ||
        lat !in -90.0..90.0 || lng !in -180.0..180.0
    ) return null

    return MapFeedbackMarker(
        localId = localId,
        latitude = lat,
        longitude = lng,
        transportType = type,
        line = line?.trim().orEmpty().ifBlank { "Unknown line" },
        overallRating = overallRating(),
        publicUsername = createdByUsername?.trim()?.takeIf(String::isNotEmpty) ?: "anonymous",
        createdAt = createdAt
    )
}

fun visibleMapMarkers(
    feedback: List<FeedbackEntity>,
    filter: MapFilter
): List<MapFeedbackMarker> = feedback.mapNotNull(FeedbackEntity::toMapFeedbackMarker)
    .filter { filter.transportType == null || it.transportType == filter.transportType }
