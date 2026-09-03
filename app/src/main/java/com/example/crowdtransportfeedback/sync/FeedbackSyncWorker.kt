package com.example.crowdtransportfeedback.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.crowdtransportfeedback.data.local.DatabaseProvider
import com.example.crowdtransportfeedback.data.remote.RetrofitClient
import com.example.crowdtransportfeedback.data.repository.FeedbackRepository

class FeedbackSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = FeedbackRepository(
            DatabaseProvider.getDatabase(applicationContext).feedbackDao(),
            RetrofitClient.api
        )
        return try {
            if (repository.synchronize().transientFailure) Result.retry() else Result.success()
        } catch (_: Exception) {
            // An unexpected failure must not be reported as a successful synchronization.
            Result.retry()
        }
    }
}
