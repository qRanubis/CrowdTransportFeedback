package com.example.crowdtransportfeedback.ui.screens

import com.example.crowdtransportfeedback.domain.BucharestTransitCatalog
import com.example.crowdtransportfeedback.domain.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineFilterTest {
    @Test
    fun numericPrefixSearchReturnsOnlyLinesStartingWithQuery() {
        val results = filterLines(BucharestTransitCatalog.linesFor(TransportType.BUS), "2")

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.startsWith("2") })
        assertFalse("102" in results)
        assertFalse("112" in results)
        assertFalse("122" in results)
    }

    @Test
    fun nightBusPrefixSearchReturnsN1Services() {
        val results = filterLines(BucharestTransitCatalog.linesFor(TransportType.NIGHT_BUS), "N1")

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.startsWith("N1") })
        assertTrue("N1" in results)
        assertTrue("N10" in results)
        assertTrue("N101" in results)
    }

    @Test
    fun metroPrefixSearchReturnsAllMetroLines() {
        val metro = BucharestTransitCatalog.linesFor(TransportType.METRO)

        assertEquals(metro, filterLines(metro, "M"))
    }

    @Test
    fun emptyQueryKeepsCatalogBrowseable() {
        val bus = BucharestTransitCatalog.linesFor(TransportType.BUS)

        assertEquals(bus, filterLines(bus, "   "))
    }
}
