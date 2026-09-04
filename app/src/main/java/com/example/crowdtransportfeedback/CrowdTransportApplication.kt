package com.example.crowdtransportfeedback

import android.app.Application
import com.example.crowdtransportfeedback.auth.AuthRepository
import com.example.crowdtransportfeedback.auth.SecureTokenStore
import com.example.crowdtransportfeedback.data.remote.RetrofitClient
import com.example.crowdtransportfeedback.sync.FeedbackSyncScheduler

class CrowdTransportApplication : Application() {
    lateinit var services: AppServices

    override fun onCreate() {
        super.onCreate()
        services = AppServices(this)
        services.network.sessionManager.restore()
        services.syncScheduler.apply {
            schedulePeriodic()
            // Also recover pending rows promptly after a process/app restart.
            scheduleOneTime()
        }
    }
}

class AppServices(application: Application) {
    val tokenStore = SecureTokenStore(application)
    val network = RetrofitClient(tokenStore)
    val syncScheduler = FeedbackSyncScheduler(application)
    val authRepository = AuthRepository(
        network.authApi,
        tokenStore,
        syncScheduler::scheduleOneTime
    )
}
