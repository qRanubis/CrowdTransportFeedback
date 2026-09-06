package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.profile.*
import com.example.crowdtransportfeedback.ui.components.*
import com.example.crowdtransportfeedback.ui.theme.SuccessContainer
import kotlinx.coroutines.launch

private sealed interface RemoteState<out T> {
    data object Loading : RemoteState<Nothing>
    data class Ready<T>(val value: T) : RemoteState<T>
    data class Failed(val message: String) : RemoteState<Nothing>
}

@Composable
fun MyProfileScreen(api: ProfileApi, onAchievements: () -> Unit, onLeaderboard: () -> Unit, onAvatarChanged: (String) -> Unit = {}) {
    var state by remember { mutableStateOf<RemoteState<ProfileDto>>(RemoteState.Loading) }
    val scope = rememberCoroutineScope()
    fun load() { scope.launch { state = runCatching { api.me() }.fold({ onAvatarChanged(it.avatarKey); RemoteState.Ready(it) }, { RemoteState.Failed("Profile unavailable. Showing no fabricated offline XP.") }) } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
      item { Text("My profile", style = MaterialTheme.typography.headlineMedium) }
      item {
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> ErrorState(current.message, ::load)
            is RemoteState.Ready -> with(current.value) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  SectionCard { Text(avatarSymbol(avatarKey), style = MaterialTheme.typography.headlineMedium); Text("@$username", style = MaterialTheme.typography.titleLarge); Text("Level ${level.level} · ${level.title}"); Text("$totalXp XP", color = MaterialTheme.colorScheme.primary); LinearProgressIndicator(progress = { if (level.maxLevel) 1f else level.xpIntoLevel.toFloat() / (level.xpIntoLevel + requireNotNull(level.xpNeededForNextLevel)).coerceAtLeast(1) }, Modifier.fillMaxWidth()); Text(if (level.maxLevel) "Maximum level reached" else "${level.xpIntoLevel} XP earned this level", style = MaterialTheme.typography.bodySmall) }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Contributions", "$contributionCount", Modifier.weight(1f)); StatCard("Lines", "$differentLineCount", Modifier.weight(1f)) }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Achievements", "$unlockedAchievementCount", Modifier.weight(1f)); StatCard("Rank", allTimeXpRank?.let { "#$it" } ?: "—", Modifier.weight(1f)) }
                  Text("Choose avatar", style = MaterialTheme.typography.titleLarge)
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("COMMUTER", "NAVIGATOR", "EXPLORER").forEach { key -> FilterChip(selected = avatarKey == key, onClick = { scope.launch { runCatching { api.avatar(mapOf("avatarKey" to key)); api.me() }.onSuccess { refreshed -> state = RemoteState.Ready(refreshed); onAvatarChanged(refreshed.avatarKey) } } }, label = { Text("${avatarSymbol(key)} ${key.lowercase().replaceFirstChar(Char::uppercase)}") }, modifier = Modifier.weight(1f)) } }
                  Text("Pinned achievements", style = MaterialTheme.typography.titleLarge)
                  if (pinnedAchievements.isEmpty()) EmptyState("Nothing pinned", "Pin up to three achievements to showcase them.") else pinnedAchievements.forEach { SectionCard { Text("🏅 ${it.title}", style = MaterialTheme.typography.titleMedium); Text(it.description, style = MaterialTheme.typography.bodySmall) } }
                }
            }
        }
      }
      item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onAchievements, Modifier.weight(1f)) { Text("Achievements") }; OutlinedButton(onClick = onLeaderboard, Modifier.weight(1f)) { Text("Leaderboard") } } }
    }
}

@Composable
fun PublicProfileScreen(api: ProfileApi, username: String) {
    var state by remember { mutableStateOf<RemoteState<ProfileDto>>(RemoteState.Loading) }
    LaunchedEffect(username) { state = runCatching { api.profile(username) }.fold({ RemoteState.Ready(it) }, { RemoteState.Failed("Public profile unavailable") }) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Public profile", style = MaterialTheme.typography.headlineMedium)
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is RemoteState.Ready -> with(current.value) { SectionCard { Text("${avatarSymbol(avatarKey)} @$username", style = MaterialTheme.typography.titleLarge); Text("Level ${level.level} · ${level.title}"); Text("$totalXp XP", color = MaterialTheme.colorScheme.primary) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Contributions", "$contributionCount", Modifier.weight(1f)); StatCard("Lines", "$differentLineCount", Modifier.weight(1f)) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("Transport types", "$transportTypeCount", Modifier.weight(1f)); StatCard("Achievements", "$unlockedAchievementCount", Modifier.weight(1f)) }; Text("Pinned achievements", style = MaterialTheme.typography.titleLarge); if (pinnedAchievements.isEmpty()) Text("No pinned achievements", color = MaterialTheme.colorScheme.onSurfaceVariant) else pinnedAchievements.forEach { SectionCard { Text("🏅 ${it.title}"); Text(it.description, style = MaterialTheme.typography.bodySmall) } } }
        }
    }
}

@Composable
fun AchievementsScreen(api: ProfileApi) {
    var state by remember { mutableStateOf<RemoteState<List<BadgeDto>>>(RemoteState.Loading) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    suspend fun refresh() { state = runCatching { api.achievements() }.fold({ RemoteState.Ready(it) }, { RemoteState.Failed("Achievements unavailable") }) }
    LaunchedEffect(Unit) { refresh() }
    Column {
      SnackbarHost(snackbar)
      when (val current = state) {
        RemoteState.Loading -> CircularProgressIndicator()
        is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
        is RemoteState.Ready -> LazyColumn(Modifier.padding(16.dp)) {
            val badges = current.value
            item { Text("Achievements", style = MaterialTheme.typography.headlineMedium); Text(achievementSummary(badges), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            badges.groupBy { it.category }.forEach { (category, list) ->
                item { Text(category, style = MaterialTheme.typography.titleLarge) }
                items(list) { badge ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = badge.unlocked) {
                        scope.launch {
                            val pins = updatedPins(badges, badge)
                            if (pins == null) {
                                snackbar.showSnackbar("You can pin up to 3 achievements.")
                            } else {
                                runCatching { api.updatePins(pins) }.onSuccess { refresh() }.onFailure { error -> snackbar.showSnackbar("Could not update pinned achievements (${error.message ?: "network error"})") }
                            }
                        }
                    }, colors = CardDefaults.cardColors(containerColor = if (badge.unlocked) SuccessContainer else MaterialTheme.colorScheme.surfaceVariant), border = if (badge.pinned) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text((if (badge.unlocked) "✓ " else "🔒 ") + badge.title, style = MaterialTheme.typography.titleMedium); Text(badge.description); if (badge.pinned) AssistChip(onClick = {}, label = { Text("Pinned") }); LinearProgressIndicator(progress = { badge.currentProgress.toFloat() / badge.targetProgress.coerceAtLeast(1) }, Modifier.fillMaxWidth()); Text("${badge.currentProgress} / ${badge.targetProgress}"); badge.unlockedAt?.let { Text("Unlocked $it", style = MaterialTheme.typography.bodySmall) } } }
                }
            }
        }
      }
    }
}

internal fun achievementSummary(badges: List<BadgeDto>): String =
    "${badges.count { it.unlocked }} of ${badges.size} unlocked"

internal fun updatedPins(badges: List<BadgeDto>, tapped: BadgeDto): List<String>? {
    val pins = badges.filter { it.pinned }.sortedBy { it.pinOrder }.map { it.code }.toMutableList()
    if (tapped.pinned) pins.remove(tapped.code) else if (pins.size == 3) return null else pins.add(tapped.code)
    return pins
}

@Composable
fun LeaderboardScreen(api: ProfileApi, onUser: (String) -> Unit) {
    var metric by remember { mutableStateOf("XP") }; var period by remember { mutableStateOf("ALL_TIME") }
    var state by remember { mutableStateOf<RemoteState<LeaderboardDto>>(RemoteState.Loading) }
    LaunchedEffect(metric, period) { state = RemoteState.Loading; state = runCatching { api.leaderboard(metric, period) }.fold({ RemoteState.Ready(it) }, { RemoteState.Failed("Leaderboard unavailable") }) }
    Column(Modifier.padding(16.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("XP", "ACHIEVEMENTS", "CONTRIBUTIONS").forEach { FilterChip(selected = metric == it, onClick = { metric = it }, label = { Text(it.lowercase().replaceFirstChar(Char::uppercase)) }) } }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("ALL_TIME", "THIS_MONTH").forEach { FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) }) } }
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is RemoteState.Ready -> { Text("Your rank: #${current.value.currentUser.rank ?: "—"}", style = MaterialTheme.typography.titleMedium); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(current.value.top) { entry -> Card(Modifier.fillMaxWidth().clickable { onUser(entry.username) }, colors = CardDefaults.cardColors(containerColor = if (entry.currentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(when(entry.rank) { 1L -> "🥇"; 2L -> "🥈"; 3L -> "🥉"; else -> "#${entry.rank}" }, Modifier.width(40.dp)); Text("${avatarSymbol(entry.avatarKey)} @${entry.username}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text("${entry.metricValue}${if (entry.currentUser) " · YOU" else ""}") } } } } }
        }
    }
}

fun avatarSymbol(key: String) = when (key) { "NAVIGATOR" -> "🧭"; "EXPLORER" -> "🗺"; else -> "🚏" }
