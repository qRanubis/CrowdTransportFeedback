package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel

@Composable
fun FeedbackDetailScreen(
    vm: FeedbackViewModel,
    id: Long,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val item by vm.getFeedbackById(id).collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { onBack() }) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        val current = item

        if (current == null) {
            Text("Loading...")
        } else {
            Text("Detail", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Score: ${current.score}")
            Text("Line: ${current.line ?: "-"}")
            Text("Comment: ${current.comment}")
            Text("lat=${current.latitude ?: "-"}  lon=${current.longitude ?: "-"}")
            Text("Synced: ${current.synced}")
            Text("Id: ${current.id}")
        }


        if (isAdmin && current != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    vm.deleteFeedbackAdmin(id) { onBack() }
                }
            ) {
                Text("Delete (admin)")
            }
        }

    }
}
