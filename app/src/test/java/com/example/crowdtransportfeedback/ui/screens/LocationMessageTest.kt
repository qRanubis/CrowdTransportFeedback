package com.example.crowdtransportfeedback.ui.screens

import com.example.crowdtransportfeedback.ui.form.LocationState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocationMessageTest {
    @Test
    fun availableLocationUsesFiveDecimalsAndDotSeparatorInRomanianLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("ro", "RO"))

            assertEquals(
                "44.92831, 25.45672",
                locationMessage(LocationState.Available(44.92831, 25.45672))
            )
        } finally {
            Locale.setDefault(previous)
        }
    }
}
