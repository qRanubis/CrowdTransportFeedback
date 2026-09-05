package com.example.crowdtransportfeedback.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdtransportfeedback.auth.AuthRepository
import com.example.crowdtransportfeedback.auth.AuthUser
import com.example.crowdtransportfeedback.auth.SessionManager
import com.example.crowdtransportfeedback.auth.SessionState
import com.example.crowdtransportfeedback.ui.screens.AccountBar
import com.example.crowdtransportfeedback.ui.screens.AddFeedbackScreen
import com.example.crowdtransportfeedback.ui.screens.AuthScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackDetailScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackListScreen
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel
import com.example.crowdtransportfeedback.profile.ProfileApi
import com.example.crowdtransportfeedback.ui.screens.*
import com.example.crowdtransportfeedback.analytics.AnalyticsRepository
import com.example.crowdtransportfeedback.data.remote.FeedbackApi
import com.example.crowdtransportfeedback.admin.AdminApi
import com.example.crowdtransportfeedback.admin.canAccessAdmin

object Routes {
    const val LIST = "list"
    const val ADD = "add"
    const val DETAIL = "detail"
    const val PROFILE = "profile"
    const val PUBLIC_PROFILE = "publicProfile"
    const val ACHIEVEMENTS = "achievements"
    const val LEADERBOARD = "leaderboard"
    const val MAP = "map"
    const val ADMIN = "admin"
}

@Composable
fun AppNav(vm: FeedbackViewModel, authRepository: AuthRepository, sessionManager: SessionManager, profileApi: ProfileApi, analyticsRepository: AnalyticsRepository, feedbackApi: FeedbackApi, adminApi: AdminApi) {
    val sessionState by sessionManager.state.collectAsState()
    when (val state = sessionState) {
        SessionState.Loading -> {
            androidx.compose.material3.CircularProgressIndicator()
            return
        }
        SessionState.Unauthenticated -> {
            AuthScreen(authRepository, sessionManager)
            return
        }
        is SessionState.Authenticated -> AuthenticatedNav(vm, state.user, sessionManager, profileApi, analyticsRepository, feedbackApi, adminApi)
    }
}

@Composable
private fun AuthenticatedNav(
    vm: FeedbackViewModel,
    user: AuthUser,
    sessionManager: SessionManager,
    profileApi: ProfileApi, analyticsRepository: AnalyticsRepository, feedbackApi: FeedbackApi, adminApi: AdminApi
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isFeedbackList = backStackEntry?.destination?.route.let { it == null || it == Routes.LIST }
    var avatarKey by remember(user.id) { mutableStateOf("COMMUTER") }
    LaunchedEffect(user.id) {
        runCatching { profileApi.me() }.onSuccess { avatarKey = it.avatarKey }
    }
    Column {
        AccountBar(
            username = user.username,
            email = user.email,
            role = user.role.name,
            session = sessionManager,
            onProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
            showBack = !isFeedbackList,
            onBack = { navController.popBackStack() },
            avatarKey = avatarKey,
            onAdmin = if (canAccessAdmin(user.role)) ({ navController.navigate(Routes.ADMIN) { launchSingleTop = true } }) else null
        )
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.weight(1f)
        ) {
            composable(Routes.LIST) {
                FeedbackListScreen(
                    vm = vm,
                    currentUserId = user.id,
                    currentUsername = user.username,
                    onAddClick = {
                        vm.resetFeedbackForm()
                        navController.navigate(Routes.ADD)
                    },
                    onMapClick = { navController.navigate(Routes.MAP) { launchSingleTop = true } },
                    onItemClick = { id -> navController.navigate("${Routes.DETAIL}/$id") }
                )
            }
            composable(Routes.MAP) {
                MapScreen(vm = vm, analyticsRepository = analyticsRepository, onFeedbackClick = { id ->
                    navController.navigate("${Routes.DETAIL}/$id")
                })
            }
            composable(Routes.ADD) {
                AddFeedbackScreen(
                    vm = vm,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.DETAIL}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                FeedbackDetailScreen(
                    vm = vm,
                    id = id,
                    currentUserId = user.id,
                    currentUsername = user.username,
                    currentUserRole = user.role,
                    feedbackApi = feedbackApi,
                    onBack = { navController.popBackStack() }
                    ,onAuthor = { navController.navigate("${Routes.PUBLIC_PROFILE}/$it") }
                )
            }
            composable(Routes.PROFILE) { MyProfileScreen(profileApi,{navController.navigate(Routes.ACHIEVEMENTS) { launchSingleTop = true }},{navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true }},{ avatarKey = it }) }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(profileApi) }
            composable(Routes.LEADERBOARD) { LeaderboardScreen(profileApi){navController.navigate("${Routes.PUBLIC_PROFILE}/$it")} }
            if (canAccessAdmin(user.role)) composable(Routes.ADMIN) {
                AdminDashboardScreen(
                    api = adminApi,
                    onFeedback = { feedbackId ->
                        val localId = runCatching { vm.resolveFeedbackLocalId(feedbackId) }.getOrNull()
                        if (localId != null) navController.navigate("${Routes.DETAIL}/$localId")
                        localId != null
                    },
                    onUser = { username -> navController.navigate("${Routes.PUBLIC_PROFILE}/$username") }
                )
            }
            composable("${Routes.PUBLIC_PROFILE}/{username}",arguments=listOf(navArgument("username"){type=NavType.StringType})){PublicProfileScreen(profileApi,it.arguments?.getString("username").orEmpty())}
        }
    }
}
