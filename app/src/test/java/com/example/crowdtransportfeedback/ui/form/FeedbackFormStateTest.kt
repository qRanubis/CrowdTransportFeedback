package com.example.crowdtransportfeedback.ui.form

import com.example.crowdtransportfeedback.domain.TransportType
import org.junit.Assert.*
import org.junit.Test

class FeedbackFormStateTest {
    private fun complete() = FeedbackFormState(
        crowdingScore = 5,
        cleanlinessScore = 3,
        punctualityScore = 4,
        transportType = TransportType.TRAM,
        line = "41",
        comment = "  useful  ",
        locationState = LocationState.Available(44.4, 26.1)
    )

    @Test
    fun ratingsAcceptOnlyOneThroughFive() {
        assertTrue(isValidRating(1))
        assertTrue(isValidRating(5))
        assertFalse(isValidRating(0))
        assertFalse(isValidRating(6))
        assertFalse(isValidRating(null))
    }

    @Test
    fun everyStructuredFieldAndAvailableLocationAreRequired() {
        val state = complete()
        assertTrue(state.isValid)
        assertFalse(state.copy(crowdingScore = null).isValid)
        assertFalse(state.copy(cleanlinessScore = null).isValid)
        assertFalse(state.copy(punctualityScore = null).isValid)
        assertFalse(state.copy(transportType = null).isValid)
        assertFalse(state.copy(line = null).isValid)
        assertFalse(state.copy(locationState = LocationState.Idle).isValid)
        assertFalse(state.copy(locationState = LocationState.PermissionDenied).isValid)
        assertFalse(state.copy(locationState = LocationState.Error).isValid)
    }

    @Test
    fun overallRatingIsCalculatedFromThreeStructuredRatings() {
        assertEquals(4.0, complete().overallRating!!, 0.0001)
        assertNull(complete().copy(crowdingScore = null).overallRating)
    }

    @Test
    fun blankCommentIsOptionalAndPersistenceValueIsTrimmed() {
        assertTrue(complete().copy(comment = "   ").isValid)
        assertEquals("useful", complete().trimmedComment())
    }

    @Test
    fun changingTransportTypeClearsLine() {
        assertNull(complete().selectTransportType(TransportType.METRO).line)
    }

    @Test
    fun availableContainsCoordinates() {
        val location = complete().locationState as LocationState.Available
        assertEquals(44.4, location.latitude, 0.0)
        assertEquals(26.1, location.longitude, 0.0)
    }

    @Test
    fun submittingStateRejectsARepeatedSubmission() {
        val accepted = complete().copy(isSubmitting = true)
        assertTrue(accepted.isSubmitting)
        assertFalse(accepted.isValid && !accepted.isSubmitting)
    }

    @Test
    fun resetForNewReportClearsFormAndStaleCoordinates() {
        val reset = complete().copy(
            isSubmitting = true,
            error = "Unable to save feedback"
        ).resetForNewReport()

        assertNull(reset.overallRating)
        assertNull(reset.crowdingScore)
        assertNull(reset.cleanlinessScore)
        assertNull(reset.punctualityScore)
        assertNull(reset.transportType)
        assertNull(reset.line)
        assertEquals("", reset.comment)
        assertEquals(LocationState.Idle, reset.locationState)
        assertFalse(reset.isSubmitting)
        assertNull(reset.error)
    }

    @Test
    fun resetForNewReportPreservesPermissionDenied() {
        val reset = complete().copy(
            locationState = LocationState.PermissionDenied
        ).resetForNewReport()

        assertEquals(LocationState.PermissionDenied, reset.locationState)
        assertNull(reset.transportType)
        assertNull(reset.line)
    }
}
