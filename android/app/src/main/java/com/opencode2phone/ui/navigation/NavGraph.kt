package com.opencode2phone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opencode2phone.ui.chat.ChatScreen
import com.opencode2phone.ui.home.HomeScreen
import com.opencode2phone.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CHAT = "chat/{sessionId}"
    const val CHAT_NEW = "chat/new"
    const val SETTINGS = "settings"

    fun chat(sessionId: String) = "chat/$sessionId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onChatClick = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onNewChatClick = {
                    navController.navigate(Routes.CHAT_NEW)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.CHAT_NEW) {
            ChatScreen(
                sessionId = null,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
