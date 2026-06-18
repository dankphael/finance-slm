package com.habibi.financeslm.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            val catalog by vm.catalog.collectAsStateWithLifecycle()
            val downloadedModels by vm.downloadedModels.collectAsStateWithLifecycle()
            val downloadStates by vm.downloadStates.collectAsStateWithLifecycle()
            val selectedModelId by vm.selectedModelId.collectAsStateWithLifecycle()

            ModelSelectionScreen(
                catalog = catalog,
                downloadedModels = downloadedModels,
                downloadStates = downloadStates,
                selectedModelId = selectedModelId,
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

            val insights by vm.insights.collectAsStateWithLifecycle()
            val loraAdapters by vm.loraAdapters.collectAsStateWithLifecycle()
            val activeLora by vm.activeLora.collectAsStateWithLifecycle()
            val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
            val generationOutput by vm.generationOutput.collectAsStateWithLifecycle()
            val generationError by vm.generationError.collectAsStateWithLifecycle()

            HomeScreen(
                insights = insights,
                loraAdapters = loraAdapters,
                activeLora = activeLora,
                isGenerating = isGenerating,
                generationOutput = generationOutput,
                generationError = generationError,
                onGenerateInsight = { vm.generateInsight() },
                onSetActiveLora = { vm.setActiveLora(it) },
                onDeleteLora = { vm.deleteLora(it) },
                onNavigateToModelManagement = { navController.navigate(Screen.ModelManagement.route) },
                onNavigateToLoraEditor = { loraId -> navController.navigate(Screen.LoraEditor.createRoute(loraId)) },
                onNavigateToPermissionsManagement = { navController.navigate(Screen.PermissionsManagement.route) },
                onExportData = { vm.exportData() },
                onDeleteAllData = { vm.deleteAllData() }
            )
        }

        // ── Settings Screens ──
        composable(Screen.ModelManagement.route) {
            val vm: ModelManagementViewModel = koinViewModel()
            val catalog by vm.catalog.collectAsStateWithLifecycle()
            val downloadedModels by vm.downloadedModels.collectAsStateWithLifecycle()
            val activeModelId by vm.activeModelId.collectAsStateWithLifecycle()
            val downloadStates by vm.downloadStates.collectAsStateWithLifecycle()
            val downloadingIds by vm.downloadingIds.collectAsStateWithLifecycle()
            val confirmDeleteModelId by vm.confirmDeleteModelId.collectAsStateWithLifecycle()

            ModelManagementScreen(
                catalog = catalog,
                downloadedModels = downloadedModels,
                activeModelId = activeModelId,
                downloadStates = downloadStates,
                downloadingIds = downloadingIds,
                confirmDeleteModelId = confirmDeleteModelId,
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
