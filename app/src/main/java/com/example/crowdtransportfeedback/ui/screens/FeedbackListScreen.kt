package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedButton
import com.example.crowdtransportfeedback.data.local.SyncState

@Composable
fun FeedbackListScreen(
    vm: FeedbackViewModel,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val list by vm.feedbackList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            Button(onClick = { onAddClick() }) {
                Text("Add feedback")
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(onClick = { vm.sync() }) {
                Text("Sync now")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onItemClick(item.localId) }
                ) {
                    Text(
                        text = listOfNotNull(item.transportType?.displayName, item.line).joinToString(" ").ifBlank { "Transport not available" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Overall trust: ${item.score}/5",
                        style = MaterialTheme.typography.bodyMedium
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

