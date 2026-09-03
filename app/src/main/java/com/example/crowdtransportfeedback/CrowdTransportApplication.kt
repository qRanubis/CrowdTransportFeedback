package com.example.crowdtransportfeedback

import android.app.Application
import com.example.crowdtransportfeedback.sync.FeedbackSyncScheduler

class CrowdTransportApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FeedbackSyncScheduler(this).apply {
            schedulePeriodic()
            // Also recover pending rows promptly after a process/app restart.
            scheduleOneTime()
        }
    }
}
