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
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("My Profile", style = MaterialTheme.typography.headlineMedium)
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is RemoteState.Ready -> with(current.value) {
                Text("${avatarSymbol(avatarKey)} @$username", style = MaterialTheme.typography.titleLarge)
                Text("Level ${level.level} · ${level.title}")
                Text(if (level.maxLevel) "$totalXp XP · MAX LEVEL" else "$totalXp XP · ${level.xpIntoLevel}/${level.xpIntoLevel + requireNotNull(level.xpNeededForNextLevel)} this level (next at ${level.nextLevelThreshold})")
                Text("$contributionCount contributions · $differentLineCount lines · $transportTypeCount types")
                Text("$unlockedAchievementCount/$achievementTotal achievements · Rank #${allTimeXpRank ?: "—"}")
                Text("Pinned: ${pinnedAchievements.joinToString { it.title }}")
                Row { listOf("COMMUTER", "NAVIGATOR", "EXPLORER").forEach { key -> TextButton(onClick = { scope.launch { runCatching { api.avatar(mapOf("avatarKey" to key)); api.me() }.onSuccess { refreshed -> state = RemoteState.Ready(refreshed); onAvatarChanged(refreshed.avatarKey) }.onFailure { state = RemoteState.Failed("Could not update avatar") } } }) { Text("${avatarSymbol(key)} $key") } } }
                contributionBreakdown.forEach { (type, count) -> Text("$type: $count") }
            }
        }
        Button(onClick = onAchievements) { Text("Achievements") }
        Button(onClick = onLeaderboard) { Text("Leaderboard") }
    }
}

@Composable
fun PublicProfileScreen(api: ProfileApi, username: String) {
    var state by remember { mutableStateOf<RemoteState<ProfileDto>>(RemoteState.Loading) }
    LaunchedEffect(username) { state = runCatching { api.profile(username) }.fold({ RemoteState.Ready(it) }, { RemoteState.Failed("Public profile unavailable") }) }
    Column(Modifier.padding(16.dp)) {
        Text("Public Profile", style = MaterialTheme.typography.headlineMedium)
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is RemoteState.Ready -> with(current.value) { Text("${avatarSymbol(avatarKey)} @$username"); Text("Level ${level.level} · ${level.title} · $totalXp XP"); Text("$contributionCount contributions · $differentLineCount lines · $transportTypeCount types"); Text("$unlockedAchievementCount achievements"); pinnedAchievements.forEach { Text("🏅 ${it.title}") } }
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
            item { Text(achievementSummary(badges), style = MaterialTheme.typography.headlineMedium) }
            badges.groupBy { it.category }.forEach { (category, list) ->
                item { Text(category, style = MaterialTheme.typography.titleLarge) }
                items(list) { badge ->
                    Card(Modifier.fillMaxWidth().padding(4.dp).clickable(enabled = badge.unlocked) {
                        scope.launch {
                            val pins = updatedPins(badges, badge)
                            if (pins == null) {
                                snackbar.showSnackbar("You can pin up to 3 achievements.")
                            } else {
                                runCatching { api.updatePins(pins) }.onSuccess { refresh() }.onFailure { error -> snackbar.showSnackbar("Could not update pinned achievements (${error.message ?: "network error"})") }
                            }
                        }
                    }) { Column(Modifier.padding(8.dp)) { Text((if (badge.unlocked) "🏅 " else "🔒 ") + badge.title); Text(badge.description); Text("${badge.currentProgress} / ${badge.targetProgress}${if (badge.pinned) " · Pinned" else ""}"); badge.unlockedAt?.let { Text("Unlocked $it") } } }
                }
            }
        }
      }
    }
}

internal fun achievementSummary(badges: List<BadgeDto>): String =
    "Achievements ${badges.count { it.unlocked }} / ${badges.size}"

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
        Row { listOf("XP", "ACHIEVEMENTS", "CONTRIBUTIONS").forEach { TextButton(onClick = { metric = it }) { Text(it) } } }
        Row { listOf("ALL_TIME", "THIS_MONTH").forEach { TextButton(onClick = { period = it }) { Text(it.replace('_', ' ')) } } }
        when (val current = state) {
            RemoteState.Loading -> CircularProgressIndicator()
            is RemoteState.Failed -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is RemoteState.Ready -> { Text("YOUR RANK #${current.value.currentUser.rank ?: "—"} @${current.value.currentUser.username} · ${current.value.currentUser.metricValue}"); LazyColumn { items(current.value.top) { entry -> Surface(color = if (entry.currentUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, tonalElevation = if (entry.currentUser) 3.dp else 0.dp) { Text("#${entry.rank} ${avatarSymbol(entry.avatarKey)} @${entry.username} · ${entry.metricValue}${if (entry.currentUser) " · YOU" else ""}", Modifier.fillMaxWidth().clickable { onUser(entry.username) }.padding(8.dp)) } } } }
        }
    }
}

fun avatarSymbol(key: String) = when (key) { "NAVIGATOR" -> "🧭"; "EXPLORER" -> "🗺"; else -> "🚏" }
