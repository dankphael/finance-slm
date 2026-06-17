package com.habibi.financeslm.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.habibi.financeslm.android.ui.viewmodel.HomeViewModel
import com.habibi.financeslm.android.ui.viewmodel.LoraEditorViewModel
import com.habibi.financeslm.android.ui.viewmodel.ModelManagementViewModel
import com.habibi.financeslm.android.ui.viewmodel.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavGraph(
    startDestination: String = Screen.Onboarding.route,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Onboarding ──
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinue = { navController.navigate(Screen.ModelSelection.route) }
            )
        }

        composable(Screen.ModelSelection.route) {
            val vm: OnboardingViewModel = koinViewModel()
            val context = LocalContext.current

            ModelSelectionScreen(
                catalog = vm.catalog.value,
                downloadedModels = vm.downloadedModels.value,
                downloadStates = vm.downloadStates.value,
                selectedModelId = vm.selectedModelId.value,
                onDownload = { modelId -> vm.downloadModel(modelId) },
                onSelect = { modelId -> vm.selectModel(modelId) },
                onContinue = {
                    vm.completeOnboarding()
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home ──
        composable(Screen.Home.route) {
            val vm: HomeViewModel = koinViewModel()

            LaunchedEffect(Unit) {
                vm.loadActiveModel()
            }

            HomeScreen(
                insights = vm.insights.value,
                loraAdapters = vm.loraAdapters.value,
                activeLora = vm.activeLora.value,
                isGenerating = vm.isGenerating.value,
                generationOutput = vm.generationOutput.value,
                generationError = vm.generationError.value,
                onGenerateInsight = { vm.generateInsight() },
                onSetActiveLora = { vm.setActiveLora(it) },
                onDeleteLora = { vm.deleteLora(it) },
                onNavigateToModelManagement = { navController.navigate(Screen.ModelManagement.route) },
                onNavigateToLoraEditor = { loraId -> navController.navigate(Screen.LoraEditor.createRoute(loraId)) },
                onNavigateToPermissionsManagement = { navController.navigate(Screen.PermissionsManagement.route) },
                onExportData = {
                    val path = vm.exportData()
                    if (path != null) {
                        android.widget.Toast.makeText(
                            navController.context,
                            "Exported to $path",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            navController.context,
                            "Export failed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDeleteAllData = {
                    vm.deleteAllData()
                    android.widget.Toast.makeText(
                        navController.context,
                        "All data deleted",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // ── Settings Screens ──
        composable(Screen.ModelManagement.route) {
            val vm: ModelManagementViewModel = koinViewModel()

            ModelManagementScreen(
                catalog = vm.catalog.value,
                downloadedModels = vm.downloadedModels.value,
                activeModelId = vm.activeModelId.value,
                downloadStates = vm.downloadStates.value,
                downloadingIds = vm.downloadingIds.value,
                confirmDeleteModelId = vm.confirmDeleteModelId.value,
                onDownloadModel = { vm.downloadModel(it) },
                onSetActiveModel = { vm.setActiveModel(it) },
                onRequestDeleteModel = { vm.requestDeleteModel(it) },
                onDismissDeleteConfirmation = { vm.dismissDeleteConfirmation() },
                onConfirmDeleteModel = { vm.confirmDeleteModel() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LoraEditor.route) { backStackEntry ->
            val loraId = backStackEntry.arguments?.getString("loraId") ?: "new"
            val vm: LoraEditorViewModel = koinViewModel()
            LoraEditorScreen(
                loraId = loraId,
                onBack = { navController.popBackStack() },
                vm = vm
            )
        }

        composable(Screen.PermissionsManagement.route) {
            PermissionsManagementScreen(onBack = { navController.popBackStack() })
        }
    }
}