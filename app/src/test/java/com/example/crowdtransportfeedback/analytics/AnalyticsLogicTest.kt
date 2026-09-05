package com.example.crowdtransportfeedback.analytics

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.domain.TransportType
import com.example.crowdtransportfeedback.ui.map.analyticsAreaGroup
import com.example.crowdtransportfeedback.ui.map.feedbackModeGroups
import com.example.crowdtransportfeedback.ui.map.geoCellId
import com.example.crowdtransportfeedback.ui.map.newestPage
import org.junit.Assert.*
import org.junit.Test

class AnalyticsLogicTest {
    private fun feedback(
        id: Long,
        state: SyncState = SyncState.SYNCED,
        type: TransportType = TransportType.METRO,
        line: String = "M2",
        time: Long = 1,
        latitude: Double = 44.4268,
        longitude: Double = 26.1025
    ) = FeedbackEntity(
        localId = id,
        feedbackId = "id$id",
        score = 3,
        comment = "",
        latitude = latitude,
        longitude = longitude,
        line = line,
        createdAt = time,
        syncState = state,
        transportType = type,
        crowdingScore = 3,
        cleanlinessScore = 3,
        punctualityScore = 3,
        createdByUsername = "u$id"
    )

    @Test fun transportChangeResetsLine() {
        assertNull(AnalyticsFilter(transportType = "METRO", line = "M2").withTransport("BUS").line)
    }

    @Test fun normalizationUsesFixedScaleAndCrowdingSemantics() {
        assertEquals(0.0, normalizedHeatWeight(1.0), 0.0)
        assertEquals(.5, normalizedHeatWeight(3.0), 0.0)
        assertEquals(1.0, normalizedHeatWeight(5.0), 0.0)
        assertTrue(AnalyticsMetric.CROWDING.legend().contains("Comfortable"))
    }

    @Test fun feedbackModeHonorsLineAndExcludesUnsynchronizedRows() {
        val groups = feedbackModeGroups(
            listOf(
                feedback(1, line = "M2", time = 1),
                feedback(2, line = "M1", time = 2),
                feedback(3, state = SyncState.PENDING_CREATE, line = "M2", time = 3)
            ),
            TransportType.METRO,
            "m2"
        )
        assertEquals(listOf(1L), groups.single().feedback.map { it.localId })
    }

    @Test fun areaListMatchesCellTypeLineAndWindow() {
        val now = 2_000_000_000_000L
        val target = feedback(1, time = now - 60_000)
        val otherLine = feedback(2, line = "M1", time = now - 60_000)
        val old = feedback(3, time = now - 8L * 24 * 60 * 60 * 1_000)
        val otherCell = feedback(4, time = now - 60_000, latitude = 44.44)
        val pending = feedback(5, state = SyncState.PENDING_CREATE, time = now - 60_000)
        val filter = AnalyticsFilter(
            window = AnalyticsWindow.D7,
            transportType = "METRO",
            line = "M2"
        )

        val group = analyticsAreaGroup(
            listOf(target, otherLine, old, otherCell, pending),
            filter,
            geoCellId(target.latitude!!, target.longitude!!)!!,
            now
        )

        assertEquals(listOf(1L), group!!.feedback.map { it.localId })
    }

    @Test fun mercatorLatitudeDomainMatchesBackend() {
        assertNotNull(geoCellId(85.05112878, 0.0))
        assertNull(geoCellId(85.05112879, 0.0))
        assertNull(geoCellId(-85.05112879, 0.0))
    }

    @Test fun incrementalPageUsesTwentyNewest() {
        val page = newestPage((1..50).toList(), { it.toLong() }, 2)
        assertEquals(40, page.size)
        assertEquals(50, page.first())
    }

    @Test fun feedbackModeHidesHeatmapError() {
        assertNull(heatmapErrorForMode(MapMode.FEEDBACK, "Heatmap unavailable"))
        assertEquals("Heatmap unavailable", heatmapErrorForMode(MapMode.HEATMAP, "Heatmap unavailable"))
    }

    @Test fun emptyMessageOnlyAppearsForCompletedEmptyHeatmap() {
        assertTrue(shouldShowHeatmapEmpty(MapMode.HEATMAP, false, null, 0))
        assertFalse(shouldShowHeatmapEmpty(MapMode.FEEDBACK, false, null, 0))
        assertFalse(shouldShowHeatmapEmpty(MapMode.HEATMAP, true, null, 0))
        assertFalse(shouldShowHeatmapEmpty(MapMode.HEATMAP, false, "unavailable", 0))
        assertFalse(shouldShowHeatmapEmpty(MapMode.HEATMAP, false, null, 1))
    }
}
