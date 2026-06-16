package com.habibi.financeslm.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.LoraAdapter
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
            title = { Text("Delete Model") },
            text = { Text("Are you sure you want to delete \"$modelName\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteModel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Management") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Downloaded Models Section ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Downloaded Models",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (downloadedModels.isEmpty()) {
                item {
                    Text(
                        "No models downloaded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Available Models",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Text(
                    "Models not yet downloaded — tap to download.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val notDownloaded = catalog.filter { c -> downloadedModels.none { it.id == c.id } }
            if (notDownloaded.isEmpty()) {
                item {
                    Text(
                        "All catalog models are downloaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
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

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isActive) {
                    FilledTonalButton(onClick = onSetActive, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set as Active")
                    }
                } else {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), enabled = false) {
                        Text("Active")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
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
    val sizeText = when {
        model.sizeBytes >= 1_000_000_000 -> "${model.sizeBytes / 1_000_000_000}.${(model.sizeBytes % 1_000_000_000) / 100_000_000}GB"
        model.sizeBytes >= 1_000_000 -> "${model.sizeBytes / 1_000_000}MB"
        else -> "${model.sizeBytes / 1_000}KB"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "~$sizeText • ${model.quantization} • ${model.contextSize} ctx",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = downloadState) {
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { if (state.progress >= 0f) state.progress else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = if (state.progress >= 0f) (state.progress * 100).toInt() else 0
                    Text(
                        "Downloading... $percent%",
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
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
}

// ── LoraEditorScreen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoraEditorScreen(
    loraId: String,
    onBack: () -> Unit,
    vm: LoraEditorViewModel
) {
    // Load existing lora on first composition
    LaunchedEffect(loraId) {
        if (loraId != "new") {
            vm.loadExisting(loraId)
        }
    }

    // Navigate back on save/delete complete
    LaunchedEffect(vm.saveComplete.value) {
        if (vm.saveComplete.value) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loraId == "new") "New LoRA" else "Edit LoRA") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                if (loraId == "new") "Create a new prompt-based LoRA adapter"
                else "Editing: $loraId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = vm.name.value,
                onValueChange = { vm.updateName(it) },
                label = { Text("Name") },
                placeholder = { Text("e.g., Aggressive Investor") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = vm.instructionText.value,
                onValueChange = { vm.updateInstructionText(it) },
                label = { Text("Instruction Text") },
                placeholder = { Text("Describe how the AI should behave...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                minLines = 5,
                maxLines = 15
            )

            // Error display
            vm.error.value?.let { errorMessage ->
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.isSaving.value
            ) {
                Text(if (vm.isSaving.value) "Saving..." else "Save")
            }

            if (loraId != "new") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.delete() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
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
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Manage permissions for screen reading.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Accessibility Service status is configured in System Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Accessibility Service",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Finance SLM reads screen content via Android Accessibility Service. Data never leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "To enable: Settings → Accessibility → Installed Apps → Finance SLM",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}