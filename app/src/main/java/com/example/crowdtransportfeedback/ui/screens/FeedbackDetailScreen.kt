package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.auth.UserRole
import com.example.crowdtransportfeedback.auth.canDeleteFeedback
import com.example.crowdtransportfeedback.data.local.SyncState
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import kotlinx.coroutines.launch
import com.example.crowdtransportfeedback.admin.canReportFeedback
import com.example.crowdtransportfeedback.admin.reportValidationError
import com.example.crowdtransportfeedback.admin.reportStatusLabel
import java.util.Locale
import com.example.crowdtransportfeedback.ui.components.SectionCard
import com.example.crowdtransportfeedback.ui.components.StatusChip

@Composable
fun FeedbackDetailScreen(
    vm: FeedbackViewModel,
    id: Long,
    currentUserId: String,
    currentUsername: String,
    currentUserRole: UserRole,
    feedbackApi: FeedbackApi,
    onBack: () -> Unit,
    onAuthor: (String) -> Unit = {}
) {
    val item by vm.getFeedbackById(id).collectAsState(initial = null)
    var isDeleting by rememberSaveable(id) { mutableStateOf(false) }
    var reportStatus by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var reportOpen by rememberSaveable(id) { mutableStateOf(false) }
    var reportError by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var deleteError by rememberSaveable(id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val current = item
        if (current == null || !current.isVisibleTo(currentUserId)) {
            Text("Feedback not available")
        } else {
            Text("Feedback detail", style = MaterialTheme.typography.headlineMedium)
            SectionCard { Row { Text("${current.transportType?.displayName ?: "Transport"} ${current.line ?: ""}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); Text(String.format(Locale.US, "★ %.1f", current.overallRating()), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }; StatusChip(current.syncState, current.rejectionReason) }
            SectionCard { Text("Trip ratings", style = MaterialTheme.typography.titleMedium); MetricRow("Punctuality", current.punctualityScore); HorizontalDivider(); MetricRow("Cleanliness", current.cleanlinessScore); HorizontalDivider(); MetricRow("Crowding comfort", current.crowdingScore) }

            val author = current.createdByUsername?.takeIf { it.isNotBlank() }
                ?: if (current.createdByUserId == currentUserId) currentUsername.takeIf { it.isNotBlank() } else null
            SectionCard(Modifier.clickable(enabled=author!=null){author?.let(onAuthor)}) { Text("Author", style = MaterialTheme.typography.labelMedium); Text("${avatarSymbol(current.createdByAvatarKey ?: "COMMUTER")} ${author?.let { "@$it  ›" } ?: "Not available"}", style = MaterialTheme.typography.titleMedium) }
            SectionCard { Text("Comment", style = MaterialTheme.typography.titleMedium); Text(current.comment.ifBlank { "No comment provided." }); if (current.latitude != null && current.longitude != null) Text(String.format(Locale.US, "Location · %.4f, %.4f", current.latitude, current.longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            val canReport = canReportFeedback(currentUserRole, currentUserId, current.createdByUserId, current.syncState == SyncState.SYNCED)
            LaunchedEffect(current.feedbackId, canReport) {
                if (canReport) runCatching { feedbackApi.myReport(current.feedbackId) }.onSuccess { reportStatus = if (it.reported) it.status else null }
            }
            if (canReport) {
                Spacer(modifier = Modifier.height(12.dp))
                if (reportStatus != null) Text(reportStatusLabel(reportStatus), color = MaterialTheme.colorScheme.primary)
                else OutlinedButton(onClick = { reportOpen = true }) { Text("Report feedback") }
                reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            val canDelete = canDeleteFeedback(currentUserRole, currentUserId, current.createdByUserId)
            if (canDelete) {
                Spacer(modifier = Modifier.height(12.dp))
                deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting,
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            if (currentUserRole == UserRole.ADMIN) {
                                vm.deleteFeedbackImmediatelyAsAdmin(id) { success ->
                                    if (success) onBack() else {
                                        isDeleting = false
                                        deleteError = "Administrator deletion requires a connection."
                                    }
                                }
                            } else vm.deleteFeedback(id) { onBack() }
                        }
                    }
                ) {
                    val isOwn = current.createdByUserId == currentUserId
                    Text(if (currentUserRole == UserRole.ADMIN && !isOwn) "Delete (admin)" else "Delete")
                }
            }
        }
    }
    if (reportOpen) ReportFeedbackDialog(onDismiss={reportOpen=false}) { reason, details ->
        scope.launch {
            val response=runCatching { feedbackApi.report(item!!.feedbackId,FeedbackApi.ReportRequest(reason,details.ifBlank { null })) }
            if(response.getOrNull()?.isSuccessful==true){reportStatus="PENDING";reportOpen=false;reportError=null}else reportError="Could not submit report. Reporting requires a connection."
        }
    }
}

@Composable
private fun MetricRow(label: String, score: Int?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(score?.let { "$it / 5" } ?: "—", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun ReportFeedbackDialog(onDismiss:()->Unit,onSubmit:(String,String)->Unit){
    val reasons=listOf("SPAM","FAKE_OR_MISLEADING","ABUSIVE_OR_INAPPROPRIATE","IRRELEVANT","DUPLICATE","OTHER")
    var reason by rememberSaveable{mutableStateOf(reasons.first())};var details by rememberSaveable{mutableStateOf("")}
    val valid=reportValidationError(reason,details)==null
    AlertDialog(onDismissRequest=onDismiss,title={Text("Report feedback")},text={Column{reasons.forEach{Row(Modifier.fillMaxWidth().clickable{reason=it}){RadioButton(reason==it,{reason=it});Text(it.replace('_',' '),Modifier.padding(top=12.dp))}};OutlinedTextField(details,{if(it.length<=250)details=it},label={Text("Details (optional)")},supportingText={Text("${details.length} / 250")})}},confirmButton={Button(enabled=valid,onClick={onSubmit(reason,details)}){Text("Submit report")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

private val SyncState.displayName: String
    get() = when (this) {
        SyncState.PENDING_CREATE -> "Pending"
        SyncState.SYNCED -> "Synchronized"
        SyncState.PENDING_DELETE -> "Pending deletion"
        SyncState.REJECTED -> "Rejected"
    }
