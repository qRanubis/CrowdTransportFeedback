package com.example.crowdtransportfeedback.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdtransportfeedback.ui.screens.AddFeedbackScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackDetailScreen
import com.example.crowdtransportfeedback.ui.screens.FeedbackListScreen
import com.example.crowdtransportfeedback.ui.viewmodel.FeedbackViewModel

object Routes {
    const val LIST = "list"
    const val ADD = "add"
    const val DETAIL = "detail"
}

@Composable
fun AppNav(vm: FeedbackViewModel, isAdmin: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            FeedbackListScreen(
                vm = vm,
                onAddClick = { navController.navigate(Routes.ADD) },
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
                isAdmin = isAdmin,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
