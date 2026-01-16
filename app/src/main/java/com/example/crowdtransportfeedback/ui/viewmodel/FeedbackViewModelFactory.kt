package com.example.crowdtransportfeedback.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.crowdtransportfeedback.data.repository.FeedbackRepository

class FeedbackViewModelFactory(
    private val repo: FeedbackRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
