package com.example.crowdtransportfeedback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.crowdtransportfeedback.data.local.DatabaseProvider
import com.example.crowdtransportfeedback.data.repository.FeedbackRepository
import com.example.crowdtransportfeedback.ui.theme.CrowdTransportFeedbackTheme
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModelFactory
import com.example.crowdtransportfeedback.ui.navigation.AppNav
import com.example.crowdtransportfeedback.sync.FeedbackSyncScheduler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // build dependencies
        val db = DatabaseProvider.getDatabase(this)
        val syncScheduler = FeedbackSyncScheduler(this)
        val app = application as CrowdTransportApplication
        val repo = FeedbackRepository(
            db.feedbackDao(),
            app.services.network.feedbackApi,
            syncScheduler::scheduleOneTime,
            app.services.network.sessionManager::hasTemporaryRefreshFailure
        )
        val factory = FeedbackViewModelFactory(repo)
        val vm: FeedbackViewModel = ViewModelProvider(this, factory)[FeedbackViewModel::class.java]
        setContent {
            CrowdTransportFeedbackTheme {
                AppNav(vm = vm, authRepository = app.services.authRepository, sessionManager = app.services.network.sessionManager)
            }
        }
    }
}
