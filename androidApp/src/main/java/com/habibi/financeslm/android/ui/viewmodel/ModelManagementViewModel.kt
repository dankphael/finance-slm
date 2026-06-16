package com.habibi.financeslm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.domain.usecase.DeleteModelUseCase
import com.habibi.financeslm.domain.usecase.DownloadModelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Model Management screen.
 *
 * Shows downloaded models, allows setting active model, deleting models,
 * and downloading new models from the catalog.
 */
class ModelManagementViewModel(
    private val downloadModelUseCase: DownloadModelUseCase,
    private val deleteModelUseCase: DeleteModelUseCase
) : ViewModel() {

    /** Catalog of all available models */
    val catalog: StateFlow<List<ModelInfo>> = downloadModelUseCase.getAvailableModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Downloaded models */
    val downloadedModels: StateFlow<List<ModelInfo>> = downloadModelUseCase.getDownloadedModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Active model id */
    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()

    /** Download states per model id */
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    /** Models currently being downloaded */
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    /** Confirmation dialog state */
    private val _confirmDeleteModelId = MutableStateFlow<String?>(null)
    val confirmDeleteModelId: StateFlow<String?> = _confirmDeleteModelId.asStateFlow()

    init {
        viewModelScope.launch {
            // Load active model
            val selected = downloadModelUseCase.let { uc ->
                // Get selected model from the repository
                kotlinx.coroutines.delay(50)
            }
        }
    }

    /**
     * Start downloading a model from the catalog.
     */
    fun downloadModel(modelId: String) {
        if (_downloadingIds.value.contains(modelId)) return

        viewModelScope.launch {
            _downloadingIds.value = _downloadingIds.value + modelId

            downloadModelUseCase.startDownload(modelId).collect { state ->
                _downloadStates.value = _downloadStates.value + (modelId to state)

                if (state is DownloadState.Done || state is DownloadState.Error) {
                    _downloadingIds.value = _downloadingIds.value - modelId
                }
            }
        }
    }

    /**
     * Show delete confirmation dialog.
     */
    fun requestDeleteModel(modelId: String) {
        _confirmDeleteModelId.value = modelId
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteConfirmation() {
        _confirmDeleteModelId.value = null
    }

    /**
     * Confirm deletion of a model.
     */
    fun confirmDeleteModel() {
        val modelId = _confirmDeleteModelId.value ?: return
        viewModelScope.launch {
            try {
                deleteModelUseCase(modelId)
                if (_activeModelId.value == modelId) {
                    _activeModelId.value = null
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
        _confirmDeleteModelId.value = null
    }

    /**
     * Set the active model.
     */
    fun setActiveModel(modelId: String) {
        viewModelScope.launch {
            downloadModelUseCase.selectModel(modelId)
            _activeModelId.value = modelId
        }
    }

    /**
     * Get download state for a specific model.
     */
    fun getDownloadState(modelId: String): DownloadState {
        return _downloadStates.value[modelId]
            ?: if (downloadedModels.value.any { it.id == modelId }) DownloadState.Done
            else DownloadState.Idle
    }

    /**
     * Check if a model is currently downloading.
     */
    fun isDownloading(modelId: String): Boolean = _downloadingIds.value.contains(modelId)
}