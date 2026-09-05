package com.example.crowdtransportfeedback.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.admin.*
import com.example.crowdtransportfeedback.domain.TransportType
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(api: AdminApi, onFeedback: suspend (String) -> Boolean, onUser: (String) -> Unit) {
    val tabs = listOf("Overview", "Reports", "Feedback", "Users", "Reporting")
    var selected by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        ScrollableTabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { index, title -> Tab(selected == index, { selected = index }, text = { Text(title) }) }
        }
        when (selected) {
            0 -> OverviewTab(api)
            1 -> ReportsTab(api)
            2 -> FeedbackTab(api, onFeedback)
            3 -> UsersTab(api, onUser)
            else -> ReportingTab(api)
        }
    }
}

@Composable
private fun <T> OnlinePanel(key: Any? = Unit, load: suspend () -> T, content: @Composable (T, () -> Unit) -> Unit) {
    var value by remember(key) { mutableStateOf<T?>(null) }
    var failed by remember(key) { mutableStateOf(false) }
    var refresh by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, refresh) {
        failed = false
        runCatching { load() }.onSuccess { value = it }.onFailure { failed = true }
    }
    when {
        failed -> Column(Modifier.padding(16.dp)) {
            Text("Admin dashboard requires a connection.")
            Button({ refresh++ }) { Text("Try again") }
        }
        value == null -> Box(Modifier.fillMaxSize().padding(24.dp)) { CircularProgressIndicator() }
        else -> content(value!!, { refresh++ })
    }
}

@Composable
private fun OverviewTab(api: AdminApi) = OnlinePanel(load = { api.overview() }) { overview, _ ->
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Text("Users: ${overview.totalUsers}")
            Text("Feedback: ${overview.totalFeedbacks}")
            Text("Last 24h / 7d / 30d: ${overview.feedbackLast24h} / ${overview.feedbackLast7d} / ${overview.feedbackLast30d}")
            Text("Active contributors (30d): ${overview.activeContributors30d}")
            Text("Pending reports: ${overview.pendingReports} across ${overview.reportedFeedbackAwaitingReview} feedbacks")
            Spacer(Modifier.height(12.dp))
            Text("Feedback by transport type", style = MaterialTheme.typography.titleMedium)
            if (overview.feedbackByTransportType.isEmpty()) Text("No feedback")
            overview.feedbackByTransportType.toSortedMap().forEach { (type, count) -> Text("$type — $count") }
            Spacer(Modifier.height(12.dp))
            Text("Top lines", style = MaterialTheme.typography.titleMedium)
            overview.topLines.forEach { Text("${it.transportType} / ${it.line} — ${it.count}") }
        }
    }
}

@Composable
private fun ReportsTab(api: AdminApi) {
    var page by remember { mutableIntStateOf(0) }
    OnlinePanel(key = page, load = { api.reports(page) }) { result, reload ->
        Column {
            if (result.content.isEmpty()) Text("No feedback awaiting review", Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f, fill = result.content.isNotEmpty())) {
                items(result.content, key = { it.feedbackId }) { row -> ModerationCard(api, row, reload) }
            }
            Pager(result.page, result.totalPages, { page-- }, { page++ })
        }
    }
}

@Composable
private fun ModerationCard(api: AdminApi, row: QueueItem, reload: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<ModerationDetail?>(null) }
    Card(Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("${row.transportType} / ${row.line}", style = MaterialTheme.typography.titleMedium)
            Text("@${row.authorUsername} · ${row.reportCount} pending reports")
            Text(row.reasonCounts.entries.joinToString { "${it.key}: ${it.value}" })
            TextButton({ scope.launch { detail = runCatching { api.detail(row.feedbackId) }.getOrNull() } }) { Text("Inspect reports") }
            detail?.let {
                Divider()
                Text("Author: @${it.feedback.createdByUsername ?: row.authorUsername}")
                Text("${it.feedback.transportType?.name ?: row.transportType} / ${it.feedback.line ?: row.line}")
                Text("Overall score: ${it.feedback.overallRating ?: it.feedback.score}")
                it.feedback.comment?.takeIf(String::isNotBlank)?.let { comment -> Text("Comment: $comment") }
                Spacer(Modifier.height(8.dp))
                it.reports.forEach { report ->
                    Text("@${report.reporterUsername}: ${report.reason}")
                    report.details?.takeIf(String::isNotBlank)?.let { details -> Text(details) }
                }
            }
            Row {
                Button({ scope.launch { if (api.resolve(row.feedbackId, ResolveRequest("KEEP")).isSuccessful) reload() } }) { Text("Keep") }
                Spacer(Modifier.width(8.dp))
                Button({ confirmDelete = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete reported feedback?") },
        text = { Text("This confirms all pending reports and cannot be undone.") },
        confirmButton = { Button({ scope.launch { if (api.resolve(row.feedbackId, ResolveRequest("DELETE")).isSuccessful) reload(); confirmDelete = false } }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } }
    )
}

@Composable
private fun FeedbackTab(api: AdminApi, onFeedback: suspend (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var filters by remember { mutableStateOf(AdminFilterState()) }
    var navigationError by remember { mutableStateOf(false) }
    val key = listOf(filters.window, filters.transportType, filters.line, filters.username, filters.page)
    Column {
        AdminFilters(filters, showUsername = true) { filters = it }
        if (navigationError) Text("Feedback detail could not be loaded. Try again.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp))
        OnlinePanel(key = key, load = { api.feedback(filters.transportType, filters.line, filters.window, filters.username.ifBlank { null }, filters.page) }) { result, _ ->
            Column(Modifier.weight(1f)) {
                if (result.content.isEmpty()) Text("No feedback", Modifier.padding(16.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(result.content) { row ->
                        ListItem(
                            headlineContent = { Text("${row.transportType} / ${row.line} · ${row.score}") },
                            supportingContent = { Text("@${row.username}${row.comment?.let { " — $it" }.orEmpty()}") },
                            modifier = Modifier.clickable { scope.launch { navigationError = !onFeedback(row.feedbackId) } }
                        )
                    }
                }
                Pager(result.page, result.totalPages, { filters = filters.copy(page = filters.page - 1) }, { filters = filters.copy(page = filters.page + 1) })
            }
        }
    }
}

@Composable
private fun UsersTab(api: AdminApi, onUser: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    Column {
        OutlinedTextField(query, { query = it; page = 0 }, label = { Text("Search username") }, modifier = Modifier.fillMaxWidth().padding(8.dp), singleLine = true)
        OnlinePanel(key = query to page, load = { api.users(query.ifBlank { null }, page) }) { result, _ ->
            Column(Modifier.weight(1f)) {
                if (result.content.isEmpty()) Text("No users", Modifier.padding(16.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(result.content) { user ->
                        ListItem(
                            headlineContent = { Text("@${user.username} · ${user.role}") },
                            supportingContent = { Text("${user.feedbackCount} feedback · ${user.totalXp} XP · level ${user.level} · ${user.verifiedReportCount} verified reports") },
                            modifier = Modifier.clickable { onUser(user.username) }
                        )
                    }
                }
                Pager(result.page, result.totalPages, { page-- }, { page++ })
            }
        }
    }
}

@Composable
private fun ReportingTab(api: AdminApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filters by remember { mutableStateOf(AdminFilterState()) }
    var csv by remember { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { output -> csv?.let(output::write) } }
    }
    Column {
        AdminFilters(filters, showUsername = false) { filters = it }
        val key = listOf(filters.window, filters.transportType, filters.line)
        OnlinePanel(key = key, load = { api.summary(filters.window, filters.transportType, filters.line) }) { summary, _ ->
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Feedback: ${summary.feedbackCount}")
                Text("Unique contributors: ${summary.uniqueContributors}")
                Text("Average overall: ${summary.averageOverall ?: "—"}")
                Text("Punctuality / cleanliness / crowding: ${summary.averagePunctuality ?: "—"} / ${summary.averageCleanliness ?: "—"} / ${summary.averageCrowding ?: "—"}")
                Text("Most active: ${summary.mostActiveTransportType ?: "—"} · ${summary.mostActiveLine ?: "—"}")
                Spacer(Modifier.height(16.dp))
                Button({ scope.launch { runCatching { api.csv(filters.window, filters.transportType, filters.line).bytes() }.onSuccess { csv = it; launcher.launch("feedback-export.csv") } } }) { Text("Export CSV") }
            }
        }
    }
}

@Composable
private fun AdminFilters(state: AdminFilterState, showUsername: Boolean, onChange: (AdminFilterState) -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row { listOf("D1", "D7", "D30", "ALL").forEach { window -> FilterChip(state.window == window, { onChange(state.withWindow(window)) }, { Text(window) }, modifier = Modifier.padding(end = 4.dp)) } }
        Row {
            ChoiceMenu("Transport", state.transportType, listOf(null) + TransportType.entries.map { it.name }) { onChange(state.withTransport(it)) }
            Spacer(Modifier.width(8.dp))
            val type = state.transportType?.let { runCatching { TransportType.valueOf(it) }.getOrNull() }
            val lineOptions = adminLineOptions(type)
            ChoiceMenu("Line", state.line, lineOptions, enabled = type != null) { onChange(state.withLine(it)) }
        }
        if (showUsername) OutlinedTextField(state.username, { onChange(state.withUsername(it)) }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ChoiceMenu(label: String, value: String?, options: List<String?>, enabled: Boolean = true, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = if (label == "Line") "All lines" else "All $label"
    Box {
        OutlinedButton({ expanded = true }, enabled = enabled) { Text(value ?: allLabel) }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem({ Text(option ?: allLabel) }, { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun Pager(page: Int, totalPages: Int, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(previous, enabled = canGoPrevious(page)) { Text("Previous") }
        Text("Page ${if (totalPages == 0) 0 else page + 1} of $totalPages", modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(next, enabled = canGoNext(page, totalPages)) { Text("Next") }
    }
}
