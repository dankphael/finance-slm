package com.habibi.financeslm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.domain.usecase.DownloadModelUseCase
import com.habibi.financeslm.domain.usecase.OnboardingStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the onboarding flow (welcome, model selection, permissions).
 */
class OnboardingViewModel(
    private val downloadModelUseCase: DownloadModelUseCase,
    private val onboardingStateUseCase: OnboardingStateUseCase
) : ViewModel() {

    /** Model catalog */
    val catalog: StateFlow<List<ModelInfo>> = downloadModelUseCase.getAvailableModels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Downloaded models */
    val downloadedModels: StateFlow<List<ModelInfo>> = downloadModelUseCase.getDownloadedModels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Download progress per model id */
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    /** Currently selected model id */
    private val _selectedModelId = MutableStateFlow<String?>(null)
    val selectedModelId: StateFlow<String?> = _selectedModelId.asStateFlow()

    /** Whether onboarding is complete */
    val isOnboardingComplete = onboardingStateUseCase.isOnboardingComplete()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        viewModelScope.launch {
            val selected = downloadModelUseCase.let {
                // Get selected model from repository
                kotlinx.coroutines.delay(100) // allow catalog to load
            }
        }
    }

    /**
     * Load the catalog from the provided JSON content (read from assets).
     */
    fun loadCatalog(jsonContent: String) {
        // The catalog is loaded into the repository via a package-private method.
        // In the current architecture, we rely on the repository having been pre-loaded.
    }

    /**
     * Start downloading a model.
     */
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _downloadStates.value = _downloadStates.value + (modelId to DownloadState.Idle)

            downloadModelUseCase.startDownload(modelId).collect { state ->
                _downloadStates.value = _downloadStates.value + (modelId to state)

                if (state is DownloadState.Done) {
                    // Auto-select the model after download completes
                    downloadModelUseCase.selectModel(modelId)
                    _selectedModelId.value = modelId
                }
            }
        }
    }

    /**
     * Select a model (from already-downloaded models).
     */
    fun selectModel(modelId: String) {
        viewModelScope.launch {
            downloadModelUseCase.selectModel(modelId)
            _selectedModelId.value = modelId
        }
    }

    /**
     * Check if a model is downloaded.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        return downloadedModels.value.any { it.id == modelId }
    }

    /**
     * Get download state for a model.
     */
    fun getDownloadState(modelId: String): DownloadState {
        return _downloadStates.value[modelId] ?: if (isModelDownloaded(modelId)) DownloadState.Done else DownloadState.Idle
    }

    /**
     * Complete the onboarding process.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingStateUseCase.completeOnboarding()
        }
    }
}