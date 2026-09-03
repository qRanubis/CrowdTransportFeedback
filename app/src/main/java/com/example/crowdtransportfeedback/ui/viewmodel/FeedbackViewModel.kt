package com.example.crowdtransportfeedback.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdtransportfeedback.data.local.FeedbackEntity
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.data.repository.FeedbackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedbackViewModel(
    private val repo: FeedbackRepository
) : ViewModel() {

    val feedbackList: StateFlow<List<FeedbackEntity>> =
        repo.getAllFeedback().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getFeedbackById(id: Long) = repo.getById(id)

    fun addFeedback(score: Int, comment: String, line: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val item = FeedbackEntity(
                score = score.coerceIn(1, 5),
                comment = comment,
                latitude = lat,
                longitude = lon,
                line = line,
                createdAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING_CREATE
            )
            repo.addFeedbackAndUpload(item)
        }
    }


    fun deleteFeedbackAdmin(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteFeedbackAdmin(id)
            onDone()
        }
    }



    fun sync() {
        viewModelScope.launch {
            try {
                repo.syncFromRemoteFull()
            } catch (e: Exception) {
                e.printStackTrace() // vezi eroarea in Logcat
            }
        }
    }

}
