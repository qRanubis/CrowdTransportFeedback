package com.example.crowdtransportfeedback.ui.form

import com.example.crowdtransportfeedback.domain.BucharestTransitCatalog
import com.example.crowdtransportfeedback.domain.TransportType

sealed interface LocationState {
    data object Idle : LocationState
    data object PermissionRequired : LocationState
    data object RequestingPermission : LocationState
    data object Loading : LocationState
    data class Available(val latitude: Double, val longitude: Double) : LocationState
    data object PermissionDenied : LocationState
    data object Error : LocationState
}

data class FeedbackFormState(
    val overallTrust: Int? = null,
    val crowdingScore: Int? = null,
    val cleanlinessScore: Int? = null,
    val punctualityScore: Int? = null,
    val transportType: TransportType? = null,
    val line: String? = null,
    val comment: String = "",
    val locationState: LocationState = LocationState.Idle,
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean get() = listOf(overallTrust, crowdingScore, cleanlinessScore, punctualityScore)
        .all(::isValidRating) && transportType != null && line != null &&
        line in BucharestTransitCatalog.linesFor(transportType) && locationState is LocationState.Available

    fun selectTransportType(value: TransportType) = copy(transportType = value, line = null)
    fun trimmedComment(): String = comment.trim()

    fun resetForNewReport(): FeedbackFormState = FeedbackFormState(
        locationState = if (locationState is LocationState.PermissionDenied) {
            LocationState.PermissionDenied
        } else {
            LocationState.Idle
        }
    )
}

fun isValidRating(value: Int?): Boolean = value != null && value in 1..5
