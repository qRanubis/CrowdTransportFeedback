package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.example.crowdtransportfeedback.data.local.SyncState

@Composable
fun FeedbackDetailScreen(
    vm: FeedbackViewModel,
    id: Long,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val item by vm.getFeedbackById(id).collectAsState(initial = null)
    var isDeleting by rememberSaveable(id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { onBack() },
            enabled = !isDeleting
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        val current = item

        if (current == null) {
            Text("Loading...")
        } else {
            Text("Detail", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Overall trust: ${current.score}/5")
            Text("Transport type: ${current.transportType?.displayName ?: "Not available"}")
            Text("Line: ${current.line ?: "Not available"}")
            Text("Crowding comfort: ${current.crowdingScore?.let { "$it/5" } ?: "Not available"}")
            Text("Cleanliness: ${current.cleanlinessScore?.let { "$it/5" } ?: "Not available"}")
            Text("Punctuality: ${current.punctualityScore?.let { "$it/5" } ?: "Not available"}")
            Text("Comment: ${current.comment.ifBlank { "Not available" }}")
            Text("Latitude / longitude: ${current.latitude ?: "Not available"} / ${current.longitude ?: "Not available"}")
            Text("Sync status: ${current.syncState.displayName}")
            Text("Local id: ${current.localId}")
        }

        if (isAdmin && current != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                enabled = !isDeleting,
                onClick = {
                    if (!isDeleting) {
                        isDeleting = true
                        vm.deleteFeedbackAdmin(id) { onBack() }
                    }
                }
            ) {
                Text("Delete (admin)")
            }
        }
    }
}

private val SyncState.displayName: String
    get() = when (this) {
        SyncState.PENDING_CREATE -> "Pending"
        SyncState.SYNCED -> "Synchronized"
        SyncState.PENDING_DELETE -> "Pending deletion"
    }
