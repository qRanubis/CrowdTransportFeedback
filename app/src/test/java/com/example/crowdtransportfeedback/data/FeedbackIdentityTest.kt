package com.example.crowdtransportfeedback.data

import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.remote.toDto
import com.example.crowdtransportfeedback.data.remote.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FeedbackIdentityTest {
    @Test
    fun newEntitiesReceiveUniqueDistributedIds() {
        val first = feedback()
        val second = feedback()

        assertNotEquals(first.feedbackId, second.feedbackId)
    }

    @Test
    fun dtoUsesDistributedIdAndNotRoomLocalId() {
        val entity = feedback(localId = 42, feedbackId = "feedback-uuid")

        val dto = entity.toDto()

        assertEquals("feedback-uuid", dto.id)
        assertNotEquals(entity.localId.toString(), dto.id)
    }

    @Test
    fun dtoEntityMappingPreservesFeedbackData() {
        val original = feedback(
            feedbackId = "feedback-uuid",
            score = 5,
            syncState = SyncState.PENDING_CREATE
        )

        val mapped = original.toDto().toEntity()

        assertEquals(original.feedbackId, mapped.feedbackId)
        assertEquals(original.score, mapped.score)
        assertEquals(original.comment, mapped.comment)
        assertEquals(original.line, mapped.line)
        assertEquals(original.latitude, mapped.latitude)
        assertEquals(original.longitude, mapped.longitude)
        assertEquals(original.createdAt, mapped.createdAt)
        assertEquals(SyncState.SYNCED, mapped.syncState)
        assertEquals(0L, mapped.localId)
    }

    private fun feedback(
        localId: Long = 0,
        feedbackId: String = java.util.UUID.randomUUID().toString(),
        score: Int = 4,
        syncState: SyncState = SyncState.PENDING_CREATE
    ) = FeedbackEntity(
        localId = localId,
        feedbackId = feedbackId,
        score = score,
        comment = "Reliable service",
        latitude = 44.4268,
        longitude = 26.1025,
        line = "41",
        createdAt = 1_700_000_000_000,
        syncState = syncState
    )
}
