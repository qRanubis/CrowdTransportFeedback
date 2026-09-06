package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.ui.components.EmptyState
import com.example.crowdtransportfeedback.ui.components.StatusChip
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackListScreen(vm: FeedbackViewModel, currentUserId: String, currentUsername: String, onAddClick: () -> Unit, onMapClick: () -> Unit, onItemClick: (Long) -> Unit) {
    val list by vm.feedbackList.collectAsState(); val visible = list.filter { it.isVisibleTo(currentUserId) }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddClick) {
                Text("＋ Add feedback")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent feedback", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onMapClick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Map") }
                IconButton(onClick = vm::sync, modifier = Modifier.size(48.dp)) { Text("↻", style = MaterialTheme.typography.titleLarge) }
            }
            if (visible.isEmpty()) EmptyState("No feedback yet", "Be the first to contribute information about your trip.") { Button(onClick = onAddClick) { Text("Add feedback") } }
            else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible, key = { it.localId }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { onItemClick(item.localId) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(listOfNotNull(item.transportType?.displayName, item.line).joinToString(" ").ifBlank { "Transport unavailable" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(String.format(Locale.US, "★ %.1f", item.overallRating()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            val author = item.createdByUsername?.takeIf(String::isNotBlank) ?: if (item.createdByUserId == currentUserId) currentUsername else null
                            Text(author?.let { "@$it" } ?: "Author unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusChip(item.syncState, item.rejectionReason)
                        }
                    }
                }
            }
        }
    }
}

internal fun rejectionReasonLabel(reason: String?): String = when (reason) { "feedback_cooldown" -> "Cooldown"; else -> "Synchronization rejected" }
