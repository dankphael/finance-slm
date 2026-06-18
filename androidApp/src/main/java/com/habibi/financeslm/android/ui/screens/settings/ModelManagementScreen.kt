package com.habibi.financeslm.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habibi.financeslm.android.R
import com.habibi.financeslm.android.ui.components.FinanceCard
import com.habibi.financeslm.android.ui.components.SectionHeader
import com.habibi.financeslm.android.ui.theme.Spacing
import com.habibi.financeslm.android.util.formatFileSize
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.android.ui.viewmodel.LoraEditorViewModel

// ── ModelManagementScreen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    catalog: List<ModelInfo>,
    downloadedModels: List<ModelInfo>,
    activeModelId: String?,
    downloadStates: Map<String, DownloadState>,
    downloadingIds: Set<String>,
    confirmDeleteModelId: String?,
    onDownloadModel: (String) -> Unit,
    onSetActiveModel: (String) -> Unit,
    onRequestDeleteModel: (String) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDeleteModel: () -> Unit,
    onBack: () -> Unit
) {
    // Delete confirmation dialog
    if (confirmDeleteModelId != null) {
        val modelName = downloadedModels.find { it.id == confirmDeleteModelId }?.name ?: confirmDeleteModelId
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirmation,
            title = { Text(stringResource(R.string.delete_model_title)) },
            text = { Text("Are you sure you want to delete \"$modelName\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteModel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirmation) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.model_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // ── Downloaded Models Section ──
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                SectionHeader(title = stringResource(R.string.downloaded_models))
            }

            if (downloadedModels.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_models_downloaded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }
            } else {
                items(downloadedModels) { model ->
                    DownloadedModelCard(
                        model = model,
                        isActive = activeModelId == model.id,
                        onSetActive = { onSetActiveModel(model.id) },
                        onDelete = { onRequestDeleteModel(model.id) }
                    )
                }
            }

            // ── Available Models Section ──
            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                SectionHeader(
                    title = stringResource(R.string.available_models),
                    subtitle = stringResource(R.string.available_models_body)
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }

            val notDownloaded = catalog.filter { c -> downloadedModels.none { it.id == c.id } }
            if (notDownloaded.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.all_models_downloaded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.sm)
                    )
                }
            } else {
                items(notDownloaded) { model ->
                    AvailableModelCard(
                        model = model,
                        isDownloading = downloadingIds.contains(model.id),
                        downloadState = downloadStates[model.id] ?: DownloadState.Idle,
                        onDownload = { onDownloadModel(model.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.xl)) }
        }
    }
}

@Composable
private fun DownloadedModelCard(
    model: ModelInfo,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isActive) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.active),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    model.downloadedPath ?: "Path unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (!isActive) {
                FilledTonalButton(onClick = onSetActive, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.set_as_active))
                }
            } else {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                    Text(stringResource(R.string.active))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    model: ModelInfo,
    isDownloading: Boolean,
    downloadState: DownloadState,
    onDownload: () -> Unit
) {
    val sizeText = formatFileSize(model.sizeBytes)

    FinanceCard {
        Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "$sizeText • ${model.quantization} • ${model.contextSize} ctx",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.md))

        when (val state = downloadState) {
            is DownloadState.Downloading -> {
                LinearProgressIndicator(
                    progress = { if (state.progress >= 0f) state.progress else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                val percent = if (state.progress >= 0f) (state.progress * 100).toInt() else 0
                Text(
                    "Downloading… $percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is DownloadState.Error -> {
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.retry))
                }
            }
            else -> {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDownloading
                ) {
                    Text("Download ($sizeText)")
                }
            }
        }
    }
}

// ── LoraEditorScreen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoraEditorScreen(
    loraId: String,
    onBack: () -> Unit,
    vm: LoraEditorViewModel
) {
    // Reactive state from the ViewModel
    val name by vm.name.collectAsStateWithLifecycle()
    val instructionText by vm.instructionText.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val saveComplete by vm.saveComplete.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Load existing lora on first composition
    LaunchedEffect(loraId) {
        if (loraId != "new") {
            vm.loadExisting(loraId)
        }
    }

    // Navigate back on save/delete complete
    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loraId == "new") "New LoRA" else "Edit LoRA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg)
        ) {
            Text(
                if (loraId == "new") "Create a new prompt-based LoRA adapter"
                else "Editing: $loraId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = name,
                onValueChange = { vm.updateName(it) },
                label = { Text("Name") },
                placeholder = { Text("e.g., Aggressive Investor") },
                isError = error != null && name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = instructionText,
                onValueChange = { vm.updateInstructionText(it) },
                label = { Text("Instruction Text") },
                placeholder = { Text("Describe how the AI should behave…") },
                supportingText = { Text("${instructionText.length} characters") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                minLines = 5,
                maxLines = 15
            )

            // Error display
            error?.let { errorMessage ->
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = { vm.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Saving…")
                } else {
                    Text("Save")
                }
            }

            if (loraId != "new") {
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete adapter?") },
            text = { Text("This LoRA adapter will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ── PermissionsManagementScreen ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsManagementScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg)
        ) {
            Text(
                "Manage permissions for screen reading.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                "Accessibility Service status is configured in System Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            FinanceCard {
                Text(
                    "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    "Finance SLM reads screen content via Android Accessibility Service. Data never leaves your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    "To enable: Settings → Accessibility → Installed Apps → Finance SLM",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
