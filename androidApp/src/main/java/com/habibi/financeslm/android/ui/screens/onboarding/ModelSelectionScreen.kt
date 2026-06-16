package com.habibi.financeslm.android.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import kotlinx.coroutines.delay

/**
 * Data class for catalog model UI state.
 */
private data class ModelCardState(
    val model: ModelInfo,
    val downloadState: DownloadState,
    val isSelected: Boolean,
    val isDownloaded: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    catalog: List<ModelInfo>,
    downloadedModels: List<ModelInfo>,
    downloadStates: Map<String, DownloadState>,
    selectedModelId: String?,
    onDownload: (String) -> Unit,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit
) {
    val hasDownloaded = downloadedModels.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Model") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download a Language Model",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose a model to power your financial insights.\nAll models run entirely on your device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(catalog) { model ->
                val state = downloadStates[model.id] ?:
                    if (downloadedModels.any { it.id == model.id }) DownloadState.Done
                    else DownloadState.Idle
                val isSelected = selectedModelId == model.id
                val isDownloaded = downloadedModels.any { it.id == model.id }
                val isRecommended = model.id == "qwen2.5-1.5b"

                ModelCard(
                    model = model,
                    downloadState = state,
                    isSelected = isSelected,
                    isDownloaded = isDownloaded,
                    isRecommended = isRecommended,
                    onDownload = { onDownload(model.id) },
                    onSelect = { onSelect(model.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasDownloaded
                ) {
                    Text(if (hasDownloaded) "Continue" else "Download a model to continue")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    downloadState: DownloadState,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isRecommended: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    val sizeText = when {
        model.sizeBytes >= 1_000_000_000 -> "${model.sizeBytes / 1_000_000_000}.${(model.sizeBytes % 1_000_000_000) / 100_000_000}GB"
        model.sizeBytes >= 1_000_000 -> "${model.sizeBytes / 1_000_000}MB"
        else -> "${model.sizeBytes / 1_000}KB"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) CardDefaults.cardColors(
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
                    model.name,
                    style = MaterialTheme.typography.titleLarge
                )
                if (isRecommended) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Recommended", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "~$sizeText • ${model.quantization} • ${model.contextSize} ctx",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress or buttons
            when (val state = downloadState) {
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { if (state.progress >= 0f) state.progress else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = if (state.progress >= 0f) (state.progress * 100).toInt() else 0
                    Text(
                        "Downloading... $percent% (${state.bytesDownloaded / 1024 / 1024}MB / ${state.totalBytes / 1024 / 1024}MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DownloadState.Done -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Ready") }
                            )
                        } else {
                            FilledTonalButton(
                                onClick = onSelect,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Use this model")
                            }
                        }
                    }
                }
                is DownloadState.Error -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry Download")
                    }
                }
                is DownloadState.Idle -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download ($sizeText)")
                    }
                }
            }
        }
    }
}