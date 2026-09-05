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
    val publicUsername: String
)

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
        publicUsername = createdByUsername?.trim()?.takeIf(String::isNotEmpty) ?: "anonymous"
    )
}

fun visibleMapMarkers(
    feedback: List<FeedbackEntity>,
    filter: MapFilter
): List<MapFeedbackMarker> = feedback.mapNotNull(FeedbackEntity::toMapFeedbackMarker)
    .filter { filter.transportType == null || it.transportType == filter.transportType }
