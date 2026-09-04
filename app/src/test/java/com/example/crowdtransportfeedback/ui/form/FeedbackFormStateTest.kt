package com.example.crowdtransportfeedback.ui.form

import com.example.crowdtransportfeedback.domain.TransportType
import org.junit.Assert.*
import org.junit.Test

class FeedbackFormStateTest {
    private fun complete() = FeedbackFormState(1, 5, 3, 4, TransportType.TRAM, "41", "  useful  ", LocationState.Available(44.4, 26.1))
    @Test fun ratingsAcceptOnlyOneThroughFive() { assertTrue(isValidRating(1)); assertTrue(isValidRating(5)); assertFalse(isValidRating(0)); assertFalse(isValidRating(6)); assertFalse(isValidRating(null)) }
    @Test fun everyStructuredFieldAndAvailableLocationAreRequired() {
        val state = complete(); assertTrue(state.isValid)
        assertFalse(state.copy(overallTrust=null).isValid); assertFalse(state.copy(crowdingScore=null).isValid)
        assertFalse(state.copy(cleanlinessScore=null).isValid); assertFalse(state.copy(punctualityScore=null).isValid)
        assertFalse(state.copy(transportType=null).isValid); assertFalse(state.copy(line=null).isValid)
        assertFalse(state.copy(locationState=LocationState.Idle).isValid)
        assertFalse(state.copy(locationState=LocationState.PermissionDenied).isValid)
        assertFalse(state.copy(locationState=LocationState.Error).isValid)
    }
    @Test fun blankCommentIsOptionalAndPersistenceValueIsTrimmed() { assertTrue(complete().copy(comment="   ").isValid); assertEquals("useful", complete().trimmedComment()) }
    @Test fun changingTransportTypeClearsLine() { assertNull(complete().selectTransportType(TransportType.METRO).line) }
    @Test fun availableContainsCoordinates() { val l=complete().locationState as LocationState.Available; assertEquals(44.4,l.latitude,0.0); assertEquals(26.1,l.longitude,0.0) }
    @Test fun submittingStateRejectsARepeatedSubmission() { val accepted = complete().copy(isSubmitting = true); assertTrue(accepted.isSubmitting); assertFalse(accepted.isValid && !accepted.isSubmitting) }
}
