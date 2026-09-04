package com.example.crowdtransportfeedback.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.repository.FeedbackRepository
import com.example.crowdtransportfeedback.domain.TransportType
import com.example.crowdtransportfeedback.ui.form.FeedbackFormState
import com.example.crowdtransportfeedback.ui.form.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedbackViewModel(private val repo: FeedbackRepository) : ViewModel() {
    val feedbackList: StateFlow<List<FeedbackEntity>> = repo.getAllFeedback().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    private val _formState = MutableStateFlow(FeedbackFormState())
    val formState = _formState.asStateFlow()

    fun getFeedbackById(id: Long) = repo.getById(id)
    fun setOverall(value: Int) = updateRating(value) { copy(overallTrust = value) }
    fun setCrowding(value: Int) = updateRating(value) { copy(crowdingScore = value) }
    fun setCleanliness(value: Int) = updateRating(value) { copy(cleanlinessScore = value) }
    fun setPunctuality(value: Int) = updateRating(value) { copy(punctualityScore = value) }
    private fun updateRating(value: Int, update: FeedbackFormState.() -> FeedbackFormState) {
        if (value in 1..5 && !_formState.value.isSubmitting) _formState.value = _formState.value.update()
    }
    fun setTransportType(value: TransportType) { if (!_formState.value.isSubmitting) _formState.value = _formState.value.selectTransportType(value) }
    fun setLine(value: String) { if (!_formState.value.isSubmitting) _formState.value = _formState.value.copy(line = value) }
    fun setComment(value: String) { if (!_formState.value.isSubmitting) _formState.value = _formState.value.copy(comment = value) }
    fun setLocationState(value: LocationState) { if (!_formState.value.isSubmitting) _formState.value = _formState.value.copy(locationState = value) }

    fun resetFeedbackForm() {
        if (!_formState.value.isSubmitting) {
            _formState.value = _formState.value.resetForNewReport()
        }
    }

    fun submit(onPersisted: () -> Unit) {
        val state = _formState.value
        if (!state.isValid || state.isSubmitting) return
        val location = state.locationState as LocationState.Available
        _formState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                repo.addFeedback(FeedbackEntity(
                    score = requireNotNull(state.overallTrust),
                    comment = state.trimmedComment(), latitude = location.latitude, longitude = location.longitude,
                    line = requireNotNull(state.line), createdAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_CREATE, transportType = requireNotNull(state.transportType),
                    crowdingScore = requireNotNull(state.crowdingScore), cleanlinessScore = requireNotNull(state.cleanlinessScore),
                    punctualityScore = requireNotNull(state.punctualityScore)
                ))
                _formState.value = FeedbackFormState()
                onPersisted()
            } catch (error: Exception) {
                _formState.value = state.copy(isSubmitting = false, error = "Unable to save feedback")
            }
        }
    }

    fun deleteFeedbackAdmin(id: Long, onDone: () -> Unit) = viewModelScope.launch { repo.deleteFeedbackAdmin(id); onDone() }
    fun sync() { viewModelScope.launch { try { repo.synchronize() } catch (error: Exception) { error.printStackTrace() } } }
}
