package com.example.crowdtransportfeedback.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdtransportfeedback.auth.AuthRepository
import com.example.crowdtransportfeedback.auth.AuthUser
import com.example.crowdtransportfeedback.auth.SessionManager
import com.example.crowdtransportfeedback.auth.SessionState
import com.example.crowdtransportfeedback.auth.canDeleteFeedback
import com.example.crowdtransportfeedback.ui.screens.AccountBar
import com.example.crowdtransportfeedback.ui.screens.AddFeedbackScreen
import com.example.crowdtransportfeedback.ui.screens.AuthScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackDetailScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackListScreen
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel

object Routes {
    const val LIST = "list"
    const val ADD = "add"
    const val DETAIL = "detail"
}

@Composable
fun AppNav(vm: FeedbackViewModel, authRepository: AuthRepository, sessionManager: SessionManager) {
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
        is SessionState.Authenticated -> AuthenticatedNav(vm, state.user, sessionManager)
    }
}

@Composable
private fun AuthenticatedNav(
    vm: FeedbackViewModel,
    user: AuthUser,
    sessionManager: SessionManager
) {
    val navController = rememberNavController()
    Column {
        AccountBar(user.email, user.role.name, sessionManager)
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.weight(1f)
        ) {
            composable(Routes.LIST) {
                FeedbackListScreen(
                    vm = vm,
                    onAddClick = {
                        vm.resetFeedbackForm()
                        navController.navigate(Routes.ADD)
                    },
                    onItemClick = { id -> navController.navigate("${Routes.DETAIL}/$id") }
                )
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
                    isAdmin = canDeleteFeedback(user.role),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
