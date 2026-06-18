package com.habibi.financeslm.android.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.habibi.financeslm.android.R
import com.habibi.financeslm.android.ui.components.FinanceCard
import com.habibi.financeslm.android.ui.components.OnboardingProgress
import com.habibi.financeslm.android.ui.components.SkeletonCard
import com.habibi.financeslm.android.ui.theme.Spacing
import com.habibi.financeslm.android.util.formatFileSize
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo

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
            TopAppBar(title = { Text(stringResource(R.string.model_select_title)) })
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.md)
                ) {
                    OnboardingProgress(
                        step = 2,
                        total = 3,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasDownloaded
                    ) {
                        Text(
                            if (hasDownloaded) stringResource(R.string.model_continue)
                            else stringResource(R.string.model_download_to_continue)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.model_select_header),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.model_select_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            if (catalog.isEmpty()) {
                // Loading skeletons while the catalog is being read.
                items(3) {
                    SkeletonCard()
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            } else {
                items(catalog) { model ->
                    val state = downloadStates[model.id]
                        ?: if (downloadedModels.any { it.id == model.id }) DownloadState.Done
                        else DownloadState.Idle
                    val isSelected = selectedModelId == model.id
                    val isRecommended = model.id == "qwen2.5-1.5b"

                    ModelCard(
                        model = model,
                        downloadState = state,
                        isSelected = isSelected,
                        isRecommended = isRecommended,
                        onDownload = { onDownload(model.id) },
                        onSelect = { onSelect(model.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.sm)) }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    downloadState: DownloadState,
    isSelected: Boolean,
    isRecommended: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    val sizeText = formatFileSize(model.sizeBytes)

    FinanceCard(
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(model.name, style = MaterialTheme.typography.titleLarge)
            if (isRecommended) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.recommended), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            "$sizeText • ${model.quantization} • ${model.contextSize} ctx",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            model.description,
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
                    "Downloading… $percent% (${formatFileSize(state.bytesDownloaded)} / ${formatFileSize(state.totalBytes)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is DownloadState.Done -> {
                if (isSelected) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.ready)) })
                } else {
                    FilledTonalButton(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.use_this_model))
                    }
                }
            }
            is DownloadState.Error -> {
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.retry_download))
                }
            }
            is DownloadState.Idle -> {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text("Download ($sizeText)")
                }
            }
        }
    }
}
