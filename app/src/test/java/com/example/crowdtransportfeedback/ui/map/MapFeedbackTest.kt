package com.example.crowdtransportfeedback.ui.map

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.domain.TransportType
import org.junit.Assert.*
import org.junit.Test

class MapFeedbackTest {
    @Test fun `only synchronized feedback with valid finite coordinates maps`() {
        assertNotNull(feedback().toMapFeedbackMarker())
        listOf(SyncState.PENDING_CREATE, SyncState.PENDING_DELETE, SyncState.REJECTED).forEach {
            assertNull(feedback(syncState = it).toMapFeedbackMarker())
        }
        assertNull(feedback(latitude = null).toMapFeedbackMarker())
        assertNull(feedback(longitude = null).toMapFeedbackMarker())
        assertNull(feedback(latitude = 90.1).toMapFeedbackMarker())
        assertNull(feedback(longitude = -180.1).toMapFeedbackMarker())
        assertNull(feedback(latitude = Double.NaN).toMapFeedbackMarker())
        assertNull(feedback(longitude = Double.POSITIVE_INFINITY).toMapFeedbackMarker())
        assertNotNull(feedback(latitude = 0.0, longitude = 0.0).toMapFeedbackMarker())
    }

    @Test fun `transport filters select their exact type`() {
        val feedback = TransportType.entries.mapIndexed { index, type ->
            feedback(localId = index.toLong(), transportType = type)
        }
        assertEquals(5, visibleMapMarkers(feedback, MapFilter.ALL).size)
        MapFilter.entries.filterNot { it == MapFilter.ALL }.forEach { filter ->
            val result = visibleMapMarkers(feedback, filter)
            assertEquals(1, result.size)
            assertEquals(filter.transportType, result.single().transportType)
        }
    }

    @Test fun `marker retains detail navigation and safe public content`() {
        val marker = feedback(localId = 42, createdByUsername = " commuter ").toMapFeedbackMarker()!!
        assertEquals(42, marker.localId)
        assertEquals(44.4, marker.latitude, 0.0)
        assertEquals(26.1, marker.longitude, 0.0)
        assertEquals(TransportType.BUS, marker.transportType)
        assertEquals("100", marker.line)
        assertEquals(4.0, marker.overallRating, 0.0)
        assertEquals("commuter", marker.publicUsername)
        assertEquals("anonymous", feedback(createdByUsername = " ").toMapFeedbackMarker()!!.publicUsername)
    }

    private fun feedback(
        localId: Long = 1,
        latitude: Double? = 44.4,
        longitude: Double? = 26.1,
        syncState: SyncState = SyncState.SYNCED,
        transportType: TransportType = TransportType.BUS,
        createdByUsername: String? = "commuter"
    ) = FeedbackEntity(
        localId = localId,
        score = 2,
        comment = "",
        latitude = latitude,
        longitude = longitude,
        line = "100",
        createdAt = 1,
        syncState = syncState,
        transportType = transportType,
        crowdingScore = 3,
        cleanlinessScore = 4,
        punctualityScore = 5,
        createdByUsername = createdByUsername
    )
}
