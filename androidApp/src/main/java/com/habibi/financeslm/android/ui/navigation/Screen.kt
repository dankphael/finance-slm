package com.habibi.financeslm.android.ui.navigation

/**
 * Sealed class defining all navigation destinations.
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object ModelSelection : Screen("model_selection")
    data object Permissions : Screen("permissions")
    data object Home : Screen("home")
    data object Insights : Screen("insights")
    data object Lora : Screen("lora")
    data object Settings : Screen("settings")
    data object ModelManagement : Screen("model_management")
    data object LoraEditor : Screen("lora_editor/{loraId}") {
        fun createRoute(loraId: String = "new") = "lora_editor/$loraId"
    }
    data object PermissionsManagement : Screen("permissions_management")
}