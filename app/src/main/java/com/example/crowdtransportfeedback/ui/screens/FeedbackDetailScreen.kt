package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.auth.UserRole
import com.example.crowdtransportfeedback.auth.canDeleteFeedback
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import java.util.Locale

@Composable
fun FeedbackDetailScreen(
    vm: FeedbackViewModel,
    id: Long,
    currentUserId: String,
    currentUsername: String,
    currentUserRole: UserRole,
    onBack: () -> Unit
) {
    val item by vm.getFeedbackById(id).collectAsState(initial = null)
    var isDeleting by rememberSaveable(id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, enabled = !isDeleting) { Text("Back") }
        Spacer(modifier = Modifier.height(12.dp))

        val current = item
        if (current == null || !current.isVisibleTo(currentUserId)) {
            Text("Feedback not available")
        } else {
            Text("Detail", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text(String.format(Locale.US, "Overall rating: %.1f/5", current.overallRating()))
            Text("Transport type: ${current.transportType?.displayName ?: "Not available"}")
            Text("Line: ${current.line ?: "Not available"}")
            Text("Crowding comfort: ${current.crowdingScore?.let { "$it/5" } ?: "Not available"}")
            Text("Cleanliness: ${current.cleanlinessScore?.let { "$it/5" } ?: "Not available"}")
            Text("Punctuality: ${current.punctualityScore?.let { "$it/5" } ?: "Not available"}")

            val author = current.createdByUsername?.takeIf { it.isNotBlank() }
                ?: if (current.createdByUserId == currentUserId) currentUsername.takeIf { it.isNotBlank() } else null
            Text("Author: ${author?.let { "@$it" } ?: "Not available"}")
            Text("Comment: ${current.comment.ifBlank { "Not available" }}")
            Text("Latitude / longitude: ${current.latitude ?: "Not available"} / ${current.longitude ?: "Not available"}")
            Text("Sync status: ${current.syncState.displayName}")
            Text("Local id: ${current.localId}")

            val canDelete = canDeleteFeedback(currentUserRole, currentUserId, current.createdByUserId)
            if (canDelete) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            vm.deleteFeedback(id) { onBack() }
                        }
                    }
                ) {
                    val isOwn = current.createdByUserId == currentUserId
                    Text(if (currentUserRole == UserRole.ADMIN && !isOwn) "Delete (admin)" else "Delete")
                }
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
