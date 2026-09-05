package com.example.crowdtransportfeedback.ui.map

import com.example.crowdtransportfeedback.analytics.AnalyticsFilter
import com.example.crowdtransportfeedback.analytics.AnalyticsWindow
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.domain.TransportType
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

private const val WEB_MERCATOR_MAX_LATITUDE = 85.05112878
private const val WEB_MERCATOR_RADIUS_METERS = 6_378_137.0
private const val CELL_SIZE_METERS = 250.0

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

data class MapFeedbackGroup(
    val cellId: String,
    val latitude: Double,
    val longitude: Double,
    val feedback: List<MapFeedbackMarker>
)

fun feedbackGroups(markers: List<MapFeedbackMarker>): List<MapFeedbackGroup> =
    markers.mapNotNull { marker -> geoCellId(marker.latitude, marker.longitude)?.let { it to marker } }
        .groupBy({ it.first }, { it.second })
        .map { (id, rows) ->
            val center = geoCellCenter(id)
            MapFeedbackGroup(id, center.first, center.second, rows.sortedByDescending { it.createdAt })
        }

fun feedbackModeGroups(
    feedback: List<FeedbackEntity>,
    transportType: TransportType?,
    line: String?
): List<MapFeedbackGroup> = feedbackGroups(
    feedback.mapNotNull(FeedbackEntity::toMapFeedbackMarker).filter { marker ->
        (transportType == null || marker.transportType == transportType) &&
            (line.isNullOrBlank() || marker.line.equals(line.trim(), ignoreCase = true))
    }
)

/** Returns synchronized local feedback matching the exact authoritative analytics context. */
fun analyticsAreaGroup(
    feedback: List<FeedbackEntity>,
    filter: AnalyticsFilter,
    cellId: String,
    nowMillis: Long
): MapFeedbackGroup? {
    val cutoff = when (filter.window) {
        AnalyticsWindow.H24 -> nowMillis - 24L * 60 * 60 * 1_000
        AnalyticsWindow.D7 -> nowMillis - 7L * 24 * 60 * 60 * 1_000
        AnalyticsWindow.D30 -> nowMillis - 30L * 24 * 60 * 60 * 1_000
        AnalyticsWindow.ALL -> Long.MIN_VALUE
    }
    val type = filter.transportType?.let(TransportType::valueOf)
    val markers = feedback.mapNotNull(FeedbackEntity::toMapFeedbackMarker).filter { marker ->
        geoCellId(marker.latitude, marker.longitude) == cellId &&
            (type == null || marker.transportType == type) &&
            (filter.line.isNullOrBlank() || marker.line.equals(filter.line.trim(), ignoreCase = true)) &&
            marker.createdAt in cutoff..nowMillis
    }
    return feedbackGroups(markers).singleOrNull()
}

fun geoCellId(latitude: Double, longitude: Double): String? {
    if (!latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -WEB_MERCATOR_MAX_LATITUDE..WEB_MERCATOR_MAX_LATITUDE ||
        longitude !in -180.0..180.0
    ) return null
    val x = WEB_MERCATOR_RADIUS_METERS * Math.toRadians(longitude)
    val y = WEB_MERCATOR_RADIUS_METERS * ln(tan(Math.PI / 4 + Math.toRadians(latitude) / 2))
    return "${floor(x / CELL_SIZE_METERS).toLong()}:${floor(y / CELL_SIZE_METERS).toLong()}"
}

fun geoCellCenter(id: String): Pair<Double, Double> {
    val (x, y) = id.split(":").map(String::toLong)
    val meterX = (x + .5) * CELL_SIZE_METERS
    val meterY = (y + .5) * CELL_SIZE_METERS
    return Math.toDegrees(2 * atan(exp(meterY / WEB_MERCATOR_RADIUS_METERS)) - Math.PI / 2) to
        Math.toDegrees(meterX / WEB_MERCATOR_RADIUS_METERS)
}

fun <T> newestPage(rows: List<T>, createdAt: (T) -> Long, page: Int): List<T> =
    rows.sortedByDescending(createdAt).take(page * 20)

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
    if (syncState != SyncState.SYNCED || geoCellId(lat, lng) == null) return null

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
): List<MapFeedbackMarker> = feedbackModeGroups(feedback, filter.transportType, null)
    .flatMap(MapFeedbackGroup::feedback)
