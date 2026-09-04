package com.example.crowdtransportfeedback.domain

/** Static September 2026 reference data; a backend/API catalog may replace it later. */
object BucharestTransitCatalog {
    private val bus = "100 101 102 103 104 105 106 112 116 117 122 123 125 135 137 138 139 141 143 162 163 168 178 182 185 196 203 205 216 220 221 222 223 226 227 232 241 243 246 253 282 290 301 301B 304 311 312 322 323 330 331 331B 335 343 368 381 382 385 421 422 423 424 425 425B 431 432 433 434 441 441B 442 443 443B 444 446 448 477 478 483 484 484B 485 487 605 610 619 627 640 641 642 655 737".split(" ")
    private val nightBus = listOf("N1", "N10") + (101..122).map { "N$it" }
    private val tram = "1 5 7 10 21 23 25 27 32 41 44 53 54".split(" ")
    private val trolleybus = "61 62 63 66 69 72 73 74 76 79 85 86 90 93 95 96 97".split(" ")
    private val metro = (1..5).map { "M$it" }

    fun linesFor(type: TransportType): List<String> = when (type) {
        TransportType.BUS -> bus
        TransportType.NIGHT_BUS -> nightBus
        TransportType.TRAM -> tram
        TransportType.TROLLEYBUS -> trolleybus
        TransportType.METRO -> metro
    }
}
