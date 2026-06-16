package com.habibi.financeslm.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.habibi.financeslm.android.ui.screens.home.HomeScreen
import com.habibi.financeslm.android.ui.screens.onboarding.ModelSelectionScreen
import com.habibi.financeslm.android.ui.screens.onboarding.OnboardingScreen
import com.habibi.financeslm.android.ui.screens.onboarding.PermissionsScreen
import com.habibi.financeslm.android.ui.screens.settings.LoraEditorScreen
import com.habibi.financeslm.android.ui.screens.settings.ModelManagementScreen
import com.habibi.financeslm.android.ui.screens.settings.PermissionsManagementScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinue = { navController.navigate(Screen.ModelSelection.route) }
            )
        }

        composable(Screen.ModelSelection.route) {
            ModelSelectionScreen(
                onContinue = { navController.navigate(Screen.Permissions.route) }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onContinue = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToModelManagement = { navController.navigate(Screen.ModelManagement.route) },
                onNavigateToLoraEditor = { loraId -> navController.navigate(Screen.LoraEditor.createRoute(loraId)) },
                onNavigateToPermissionsManagement = { navController.navigate(Screen.PermissionsManagement.route) }
            )
        }

        composable(Screen.ModelManagement.route) {
            ModelManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.LoraEditor.route) { backStackEntry ->
            val loraId = backStackEntry.arguments?.getString("loraId") ?: "new"
            LoraEditorScreen(loraId = loraId, onBack = { navController.popBackStack() })
        }

        composable(Screen.PermissionsManagement.route) {
            PermissionsManagementScreen(onBack = { navController.popBackStack() })
        }
    }
}