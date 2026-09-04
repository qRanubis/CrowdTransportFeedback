package com.example.crowdtransportfeedback.profile

import org.junit.Assert.*
import org.junit.Test

class ProfileModelsTest {
    @Test fun maxLevelHasNoNextTargetAndKeepsUncappedXp() {
        val level = LevelDto(15, "Urban Mobility Expert", 5000, 2420, null, null, true)
        assertTrue(level.maxLevel); assertNull(level.nextLevelThreshold); assertEquals(2420, level.xpIntoLevel)
    }

    @Test fun levelProgressFieldsUseOneConsistentContract() {
        val level = LevelDto(8, "City Navigator", 1375, 145, 230, 1750, false)
        assertEquals(1520, level.levelStartXp + level.xpIntoLevel)
        assertEquals(1750, level.nextLevelThreshold)
    }

    @Test fun leaderboardKeepsSeparateCurrentUserOutsideTop100() {
        val me = LeaderboardEntryDto(137, "rares", "COMMUTER", 8, 1520, 1520, true)
        val model = LeaderboardDto(emptyList(), me)
        assertEquals(137, model.currentUser.rank); assertTrue(model.top.isEmpty())
    }

    @Test fun pinnedOrderIsIndependentOfCatalogOrder() {
        val second = BadgeDto("B", "B", "", "Contribution", true, null, 1, 1, true, 1)
        val first = BadgeDto("A", "A", "", "Contribution", true, null, 1, 1, true, 0)
        assertEquals(listOf("A", "B"), listOf(second, first).sortedBy { it.pinOrder }.map { it.code })
    }
}
