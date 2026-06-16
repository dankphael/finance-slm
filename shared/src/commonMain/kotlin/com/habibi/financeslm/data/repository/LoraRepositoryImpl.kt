package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.domain.repository.LoraRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub LoraRepository for compilation.
 * Real implementation will use SQLDelight for persistence.
 */
class LoraRepositoryImpl : LoraRepository {
    private val _adapters = MutableStateFlow<List<LoraAdapter>>(emptyList())
    private var _activeId: String? = null
    private var nextId = 1
    private var timeCounter = System.currentTimeMillis()

    override fun getAll(): Flow<List<LoraAdapter>> = _adapters.asStateFlow()

    override suspend fun getById(id: String): LoraAdapter? {
        return _adapters.value.find { it.id == id }
    }

    override suspend fun create(name: String, instructionText: String): LoraAdapter {
        val now = timeCounter++
        val adapter = LoraAdapter(
            id = "lora_${nextId++}",
            name = name,
            instructionText = instructionText,
            createdAt = now,
            updatedAt = now
        )
        _adapters.value = _adapters.value + adapter
        Logger.d("LoraRepo", "Created: $name")
        return adapter
    }

    override suspend fun update(id: String, name: String, instructionText: String) {
        val now = timeCounter++
        _adapters.value = _adapters.value.map {
            if (it.id == id) it.copy(name = name, instructionText = instructionText, updatedAt = now)
            else it
        }
    }

    override suspend fun delete(id: String) {
        _adapters.value = _adapters.value.filter { it.id != id }
        if (_activeId == id) _activeId = null
    }

    override suspend fun getActive(): LoraAdapter? {
        return _adapters.value.find { it.id == _activeId }
    }

    override suspend fun setActive(id: String?) {
        _activeId = id
    }
}