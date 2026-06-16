package com.habibi.financeslm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.domain.usecase.ManageLoraUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the LoRA editor screen.
 *
 * Supports both creating new LoRA adapters and editing existing ones.
 */
class LoraEditorViewModel(
    private val manageLoraUseCase: ManageLoraUseCase
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _instructionText = MutableStateFlow("")
    val instructionText: StateFlow<String> = _instructionText.asStateFlow()

    private val _isNew = MutableStateFlow(true)
    val isNew: StateFlow<Boolean> = _isNew.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentLoraId: String? = null

    /**
     * Load an existing LoRA adapter for editing.
     */
    fun loadExisting(loraId: String) {
        viewModelScope.launch {
            val adapter = manageLoraUseCase.getById(loraId)
            if (adapter != null) {
                _name.value = adapter.name
                _instructionText.value = adapter.instructionText
                _isNew.value = false
                currentLoraId = adapter.id
            } else {
                _error.value = "LoRA adapter not found: $loraId"
            }
        }
    }

    /**
     * Update the name field.
     */
    fun updateName(value: String) {
        _name.value = value
    }

    /**
     * Update the instruction text field.
     */
    fun updateInstructionText(value: String) {
        _instructionText.value = value
    }

    /**
     * Save the LoRA adapter (create or update).
     */
    fun save() {
        val nameValue = _name.value.trim()
        val instructionValue = _instructionText.value.trim()

        if (nameValue.isEmpty()) {
            _error.value = "Name is required"
            return
        }
        if (instructionValue.isEmpty()) {
            _error.value = "Instruction text is required"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            try {
                if (_isNew.value) {
                    manageLoraUseCase.create(nameValue, instructionValue)
                } else {
                    currentLoraId?.let { id ->
                        manageLoraUseCase.update(id, nameValue, instructionValue)
                    }
                }
                _saveComplete.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Delete the current LoRA adapter (only for existing ones).
     */
    fun delete() {
        val id = currentLoraId ?: return
        viewModelScope.launch {
            try {
                manageLoraUseCase.delete(id)
                _saveComplete.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete"
            }
        }
    }

    /**
     * Dismiss the error.
     */
    fun dismissError() {
        _error.value = null
    }
}