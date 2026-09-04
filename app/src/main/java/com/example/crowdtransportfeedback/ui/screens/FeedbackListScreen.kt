package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import java.util.Locale

@Composable
fun FeedbackListScreen(
    vm: FeedbackViewModel,
    currentUserId: String,
    currentUsername: String,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val list by vm.feedbackList.collectAsState()
    val visibleList = list.filter { it.isVisibleTo(currentUserId) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row {
            Button(onClick = onAddClick) { Text("Add feedback") }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = vm::sync) { Text("Sync now") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(visibleList) { item ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        .clickable { onItemClick(item.localId) }
                ) {
                    Text(
                        text = listOfNotNull(item.transportType?.displayName, item.line)
                            .joinToString(" ").ifBlank { "Transport not available" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(Locale.US, "Overall rating: %.1f/5", item.overallRating()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val author = item.createdByUsername?.takeIf { it.isNotBlank() }
                        ?: if (item.createdByUserId == currentUserId) currentUsername.takeIf { it.isNotBlank() } else null
                    Text(
                        text = author?.let { "by @$it" } ?: "Author unavailable",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = item.syncState.displayName,
                        style = MaterialTheme.typography.bodySmall
                    )
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
