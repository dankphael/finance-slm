package com.habibi.financeslm.android.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.InsightCategory
import com.habibi.financeslm.domain.model.LoraAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    insights: List<FinanceInsight>,
    loraAdapters: List<LoraAdapter>,
    activeLora: LoraAdapter?,
    isGenerating: Boolean,
    generationOutput: String,
    generationError: String?,
    onGenerateInsight: () -> Unit,
    onSetActiveLora: (String?) -> Unit,
    onDeleteLora: (String) -> Unit,
    onNavigateToModelManagement: () -> Unit,
    onNavigateToLoraEditor: (String) -> Unit,
    onNavigateToPermissionsManagement: () -> Unit,
    onExportData: () -> Unit = {},
    onDeleteAllData: () -> Unit = {}
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
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "LoRA") },
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
            0 -> InsightsTab(
                modifier = Modifier.padding(padding),
                insights = insights,
                isGenerating = isGenerating,
                generationOutput = generationOutput,
                generationError = generationError,
                onGenerate = onGenerateInsight
            )
            1 -> LoraTab(
                modifier = Modifier.padding(padding),
                adapters = loraAdapters,
                activeLora = activeLora,
                onSetActive = onSetActiveLora,
                onDelete = onDeleteLora,
                onEdit = onNavigateToLoraEditor,
                onCreate = { onNavigateToLoraEditor("new") }
            )
            2 -> SettingsTab(
                modifier = Modifier.padding(padding),
                onModelManagement = onNavigateToModelManagement,
                onPermissionsManagement = onNavigateToPermissionsManagement,
                onExportData = onExportData,
                onDeleteAllData = onDeleteAllData
            )
        }
    }
}

// ── Insights Tab ────────────────────────────────────────────────────────────

@Composable
private fun InsightsTab(
    modifier: Modifier = Modifier,
    insights: List<FinanceInsight>,
    isGenerating: Boolean,
    generationOutput: String,
    generationError: String?,
    onGenerate: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (insights.isEmpty() && !isGenerating) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No insights yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Download a model and start screen reading to generate personalized financial tips.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(onClick = onGenerate, enabled = !isGenerating) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Insight")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current generation output
                if (isGenerating || generationOutput.isNotEmpty()) {
                    item {
                        if (isGenerating) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generating insight...", style = MaterialTheme.typography.titleSmall)
                                    }
                                    if (generationOutput.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            generationOutput,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Error state
                if (generationError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                generationError,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Existing insights
                items(insights) { insight ->
                    InsightCard(insight)
                }
            }
        }

        // FAB
        if (!isGenerating) {
            FloatingActionButton(
                onClick = onGenerate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

@Composable
private fun InsightCard(insight: FinanceInsight) {
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CategoryBadge(insight.category)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                insight.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (insight.sourceApp != null) {
                    Text(
                        insight.sourceApp ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    dateFormat.format(Date(insight.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: InsightCategory) {
    val (label, color) = when (category) {
        InsightCategory.SPENDING -> "Spending" to MaterialTheme.colorScheme.tertiary
        InsightCategory.SAVINGS -> "Savings" to MaterialTheme.colorScheme.primary
        InsightCategory.INVESTMENT -> "Investment" to MaterialTheme.colorScheme.secondary
        InsightCategory.BUDGET -> "Budget" to MaterialTheme.colorScheme.error
        InsightCategory.GENERAL -> "General" to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ── LoRA Tab ────────────────────────────────────────────────────────────────

@Composable
private fun LoraTab(
    modifier: Modifier = Modifier,
    adapters: List<LoraAdapter>,
    activeLora: LoraAdapter?,
    onSetActive: (String?) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (adapters.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No LoRA adapters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Create one to customize your financial advisor persona.\nLoRA adapters are prompt-based — no weight files needed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Background / LoRA Adapters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Customize your financial advisor's persona with prompt-based instructions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(adapters) { adapter ->
                    val isActive = activeLora?.id == adapter.id
                    LoraCard(
                        adapter = adapter,
                        isActive = isActive,
                        onEdit = { onEdit(adapter.id) },
                        onDelete = { onDelete(adapter.id) },
                        onSetActive = {
                            if (isActive) onSetActive(null)
                            else onSetActive(adapter.id)
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create LoRA")
        }
    }
}

@Composable
private fun LoraCard(
    adapter: LoraAdapter,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit
) {
    Card(
        colors = if (isActive) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    adapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Active") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                adapter.instructionText.lines().firstOrNull() ?: adapter.instructionText.take(100),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Edit")
                }
                if (!isActive) {
                    OutlinedButton(onClick = onSetActive, modifier = Modifier.weight(1f)) {
                        Text("Set Active")
                    }
                } else {
                    OutlinedButton(onClick = onSetActive, modifier = Modifier.weight(1f)) {
                        Text("Deactivate")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ── Settings Tab ────────────────────────────────────────────────────────────

@Composable
private fun SettingsTab(
    modifier: Modifier = Modifier,
    onModelManagement: () -> Unit = {},
    onPermissionsManagement: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Models", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Download, manage, and switch between language models.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onModelManagement, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage Models")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Accessibility", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Manage screen reader permissions and monitored apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onPermissionsManagement, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage Permissions")
                }
            }
        }
    }
}