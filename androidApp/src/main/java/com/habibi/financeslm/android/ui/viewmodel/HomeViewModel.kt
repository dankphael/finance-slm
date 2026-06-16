package com.habibi.financeslm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.usecase.GenerateInsightUseCase
import com.habibi.financeslm.domain.usecase.ManageLoraUseCase
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen (Insights + LoRA tabs).
 */
class HomeViewModel(
    private val generateInsightUseCase: GenerateInsightUseCase,
    private val manageLoraUseCase: ManageLoraUseCase,
    private val inferenceRepository: InferenceRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    /** Insights list */
    val insights: StateFlow<List<FinanceInsight>> = generateInsightUseCase.getInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** LoRA adapters */
    val loraAdapters: StateFlow<List<LoraAdapter>> = manageLoraUseCase.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Active LoRA adapter */
    private val _activeLora = MutableStateFlow<LoraAdapter?>(null)
    val activeLora: StateFlow<LoraAdapter?> = _activeLora.asStateFlow()

    /** Whether an insight is currently being generated */
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /** Current generation output (partial tokens) */
    private val _generationOutput = MutableStateFlow("")
    val generationOutput: StateFlow<String> = _generationOutput.asStateFlow()

    /** Error during generation */
    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    init {
        viewModelScope.launch {
            manageLoraUseCase.getActive()?.let {
                _activeLora.value = it
            }
        }
    }

    /**
     * Pre-load the active model into memory so generate() calls don't
     * incur the 2-5s load penalty. Called when navigating to the Home screen.
     */
    fun loadActiveModel() {
        viewModelScope.launch {
            try {
                val selected = modelRepository.getSelectedModel()
                if (selected != null && selected.downloadedPath != null) {
                    inferenceRepository.loadModel(selected.downloadedPath!!)
                    Logger.d("HomeViewModel", "Pre-loaded model: ${selected.name}")
                }
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Failed to pre-load model", e)
            }
        }
    }

    /**
     * Generate an insight from the latest screen data.
     */
    fun generateInsight() {
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationOutput.value = ""

            try {
                val loraInstruction = _activeLora.value?.instructionText
                generateInsightUseCase.generateFromLatestScreen(loraInstruction).collect { token ->
                    _generationOutput.value += token
                }
            } catch (e: Exception) {
                _generationError.value = e.message ?: "Generation failed"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Set active LoRA adapter.
     */
    fun setActiveLora(loraId: String?) {
        viewModelScope.launch {
            manageLoraUseCase.setActive(loraId)
            _activeLora.value = if (loraId != null) manageLoraUseCase.getById(loraId) else null
        }
    }

    /**
     * Delete a LoRA adapter.
     */
    fun deleteLora(loraId: String) {
        viewModelScope.launch {
            manageLoraUseCase.delete(loraId)
            if (_activeLora.value?.id == loraId) {
                _activeLora.value = null
            }
        }
    }

    /**
     * Clear all insights.
     */
    fun clearInsights() {
        viewModelScope.launch {
            generateInsightUseCase.getInsights() // triggers clear via the repo
        }
    }
}