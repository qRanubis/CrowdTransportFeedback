package com.example.crowdtransportfeedback.ui.screens

import com.example.crowdtransportfeedback.profile.BadgeDto
import org.junit.Assert.*
import org.junit.Test

class AchievementUiRulesTest {
    @Test fun fourthPinIsRejectedWithoutProducingAnApiPayload() {
        val pinned = (0..2).map { badge("P$it", pinned = true, order = it) }
        assertNull(updatedPins(pinned + badge("FOURTH"), badge("FOURTH")))
    }

    @Test fun unpinMakesAnotherAchievementPinnableAndPreservesOrder() {
        val first = badge("FIRST", true, 0)
        val second = badge("SECOND", true, 1)
        val third = badge("THIRD", true, 2)
        assertEquals(listOf("FIRST", "THIRD"), updatedPins(listOf(first, second, third), second))
        assertEquals(listOf("FIRST", "THIRD", "FOURTH"), updatedPins(listOf(first, third, badge("FOURTH")), badge("FOURTH")))
    }

    @Test fun cooldownReasonHasReadableLabel() {
        assertEquals("Cooldown", rejectionReasonLabel("feedback_cooldown"))
    }

    @Test fun achievementSummaryUsesAuthoritativeListSize() {
        val badges = (1..31).map { badge("A$it").copy(unlocked = it <= 4) }
        assertEquals("Achievements 4 / 31", achievementSummary(badges))
    }

    private fun badge(code: String, pinned: Boolean = false, order: Int? = null) =
        BadgeDto(code, code, "", "Contribution", true, null, 1, 1, pinned, order)
}
