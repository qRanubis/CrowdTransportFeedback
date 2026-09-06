package com.example.crowdtransportfeedback.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.ui.screens.rejectionReasonLabel
import com.example.crowdtransportfeedback.ui.theme.*

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) = Card(modifier) {
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatusChip(state: SyncState, rejectionReason: String? = null) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val (label, foreground, background) = when (state) {
        SyncState.SYNCED -> Triple("✓ Synchronized", if (dark) DarkSuccess else Success, if (dark) DarkSuccessContainer else SuccessContainer)
        SyncState.PENDING_CREATE -> Triple("◷ Pending", if (dark) DarkWarning else Warning, if (dark) DarkWarningContainer else WarningContainer)
        SyncState.PENDING_DELETE -> Triple("◷ Pending deletion", if (dark) DarkWarning else Warning, if (dark) DarkWarningContainer else WarningContainer)
        SyncState.REJECTED -> Triple("! Rejected · ${rejectionReasonLabel(rejectionReason)}", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
    }
    Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) =
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }

@Composable
fun ErrorState(message: String = "Check your connection and try again.", retry: (() -> Unit)? = null, modifier: Modifier = Modifier) =
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(message)
            retry?.let { TextButton(onClick = it) { Text("Retry") } }
        }
    }
