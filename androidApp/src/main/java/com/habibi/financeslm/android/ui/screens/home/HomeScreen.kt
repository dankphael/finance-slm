package com.habibi.financeslm.android.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToModelManagement: () -> Unit,
    onNavigateToLoraEditor: (String) -> Unit,
    onNavigateToPermissionsManagement: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Finance SLM") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Insights") },
                    label = { Text("Insights") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "LoRA") },
                    label = { Text("LoRA") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> InsightsTab(modifier = Modifier.padding(padding))
            1 -> LoraTab(
                modifier = Modifier.padding(padding),
                onEditLora = onNavigateToLoraEditor
            )
            2 -> SettingsTab(
                modifier = Modifier.padding(padding),
                onModelManagement = onNavigateToModelManagement,
                onPermissionsManagement = onNavigateToPermissionsManagement
            )
        }
    }
}

@Composable
private fun InsightsTab(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No insights yet. Download a model and start screen reading to generate personalized financial tips.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoraTab(
    modifier: Modifier = Modifier,
    onEditLora: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Background/LoRA", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No LoRA adapters yet. Create one to customize your financial advisor persona.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsTab(
    modifier: Modifier = Modifier,
    onModelManagement: () -> Unit = {},
    onPermissionsManagement: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onModelManagement, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Models")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onPermissionsManagement, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Permissions")
        }
    }
}