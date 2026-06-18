package com.habibi.financeslm.android.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.habibi.financeslm.android.R
import androidx.compose.ui.text.style.TextOverflow
import com.habibi.financeslm.android.ui.components.CategoryBadge
import com.habibi.financeslm.android.ui.components.EmptyState
import com.habibi.financeslm.android.ui.components.FinanceCard
import com.habibi.financeslm.android.ui.theme.Spacing
import com.habibi.financeslm.android.util.formatTimestamp
import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.LoraAdapter
import kotlinx.coroutines.launch

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
    onExportData: () -> String? = { null },
    onDeleteAllData: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val title = when (selectedTab) {
        0 -> stringResource(R.string.tab_insights)
        1 -> stringResource(R.string.tab_lora)
        else -> stringResource(R.string.tab_settings)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_insights)) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_lora)) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
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
                onExportData = {
                    val path = onExportData()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (path != null) "Exported insights to $path" else "Export failed"
                        )
                    }
                },
                onDeleteAllData = {
                    onDeleteAllData()
                    scope.launch { snackbarHostState.showSnackbar("All data deleted") }
                }
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
            EmptyState(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.insights_empty_title),
                description = stringResource(R.string.insights_empty_body),
                actionLabel = stringResource(R.string.generate_insight),
                onAction = onGenerate
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Current generation output (live streaming)
                if (isGenerating) {
                    item {
                        FinanceCard(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    stringResource(R.string.generating_insight),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (generationOutput.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    generationOutput,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Error state
                if (generationError != null) {
                    item {
                        FinanceCard(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                generationError,
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
                    .padding(Spacing.lg)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.generate_insight))
            }
        }
    }
}

@Composable
private fun InsightCard(insight: FinanceInsight) {
    FinanceCard {
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
            Spacer(modifier = Modifier.width(Spacing.sm))
            CategoryBadge(insight.category)
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            insight.summary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sm))
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
                formatTimestamp(insight.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            EmptyState(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.lora_empty_title),
                description = stringResource(R.string.lora_empty_body)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    Text(
                        stringResource(R.string.lora_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.lora_section_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
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
                .padding(Spacing.lg)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.lora_create))
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
    FinanceCard(
        colors = if (isActive) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
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
                    label = { Text(stringResource(R.string.active)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            adapter.instructionText.lines().firstOrNull() ?: adapter.instructionText.take(100),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(onClick = onSetActive, modifier = Modifier.weight(1f)) {
                Text(if (isActive) stringResource(R.string.deactivate) else stringResource(R.string.set_active))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Settings Tab ────────────────────────────────────────────────────────────

@Composable
private fun SettingsTab(
    modifier: Modifier = Modifier,
    onModelManagement: () -> Unit = {},
    onPermissionsManagement: () -> Unit = {},
    onExportData: () -> Unit = {},
    onDeleteAllData: () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        FinanceCard {
            Text(stringResource(R.string.settings_models), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_models_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Button(onClick = onModelManagement, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_manage_models))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        FinanceCard {
            Text(stringResource(R.string.settings_accessibility), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_accessibility_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Button(onClick = onPermissionsManagement, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_manage_permissions))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        FinanceCard {
            Text(stringResource(R.string.settings_data_privacy), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_data_privacy_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            OutlinedButton(onClick = onExportData, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.settings_delete_all))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_privacy_policy))
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllData()
                    showDeleteConfirmation = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
