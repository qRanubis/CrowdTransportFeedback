package com.example.crowdtransportfeedback.domain
import org.junit.Assert.*
import org.junit.Test
class BucharestTransitCatalogTest {
 @Test fun categoriesAreExactAndSeparate() {
  val bus=BucharestTransitCatalog.linesFor(TransportType.BUS); val night=BucharestTransitCatalog.linesFor(TransportType.NIGHT_BUS)
  assertFalse(bus.any { it.startsWith("N") }); assertEquals(listOf("N1","N10")+(101..122).map{"N$it"},night); assertTrue(bus.intersect(night.toSet()).isEmpty())
  assertEquals(listOf("M1","M2","M3","M4","M5"),BucharestTransitCatalog.linesFor(TransportType.METRO))
  assertEquals("1 5 7 10 21 23 25 27 32 41 44 53 54".split(" "),BucharestTransitCatalog.linesFor(TransportType.TRAM))
  assertEquals("61 62 63 66 69 72 73 74 76 79 85 86 90 93 95 96 97".split(" "),BucharestTransitCatalog.linesFor(TransportType.TROLLEYBUS))
 }
}
