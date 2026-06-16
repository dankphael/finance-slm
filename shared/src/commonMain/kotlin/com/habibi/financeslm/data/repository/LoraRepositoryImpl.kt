package com.habibi.financeslm.data.repository

import com.habibi.financeslm.data.datasource.PreferencesDataSource
import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.domain.repository.LoraRepository
import com.habibi.financeslm.prompt.SystemPrompts
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Real LoraRepository implementation.
 *
 * Stores LoRA adapters in memory (list of [LoraAdapter]) and persists the active
 * LoRA ID and the adapter list via [PreferencesDataSource].
 *
 * On first launch, pre-seeds a "Default Finance Advisor" LoRA adapter.
 */
class LoraRepositoryImpl(
    private val preferencesDataSource: PreferencesDataSource
) : LoraRepository {

    companion object {
        private const val KEY_LORA_LIST = "lora_adapters"
        private const val DEFAULT_LORA_ID = "lora_default"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _adapters = MutableStateFlow<List<LoraAdapter>>(emptyList())
    private var nextId = 1
    private var timeCounter = System.currentTimeMillis()

    init {
        // Load persisted adapters or seed with default
        val saved = loadFromPrefs()
        if (saved.isNotEmpty()) {
            _adapters.value = saved
            // Calculate nextId from existing adapters
            val maxNum = saved.mapNotNull { it.id.removePrefix("lora_").toIntOrNull() }.maxOrNull()
            if (maxNum != null) nextId = maxNum + 1
            Logger.d("LoraRepo", "Loaded ${saved.size} adapters from preferences")
        } else {
            seedDefaultAdapter()
        }
    }

    private fun loadFromPrefs(): List<LoraAdapter> {
        val raw = preferencesDataSource.getString(KEY_LORA_LIST)
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<LoraAdapter>>(raw)
        } catch (e: Exception) {
            Logger.e("LoraRepo", "Failed to parse saved adapters", e)
            emptyList()
        }
    }

    private fun saveToPrefs() {
        val raw = json.encodeToString(_adapters.value)
        preferencesDataSource.putString(KEY_LORA_LIST, raw)
    }

    /**
     * Pre-seed the default financial advisor LoRA on first launch.
     */
    private fun seedDefaultAdapter() {
        val now = timeCounter++
        val defaultAdapter = LoraAdapter(
            id = DEFAULT_LORA_ID,
            name = "Default Finance Advisor",
            instructionText = SystemPrompts().defaultFinanceAdvisor(),
            createdAt = now,
            updatedAt = now
        )
        _adapters.value = listOf(defaultAdapter)
        saveToPrefs()
        Logger.d("LoraRepo", "Seeded default LoRA adapter: $DEFAULT_LORA_ID")
    }

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
        saveToPrefs()
        Logger.d("LoraRepo", "Created: $name (${adapter.id})")
        return adapter
    }

    override suspend fun update(id: String, name: String, instructionText: String) {
        val now = timeCounter++
        _adapters.value = _adapters.value.map {
            if (it.id == id) it.copy(name = name, instructionText = instructionText, updatedAt = now)
            else it
        }
        saveToPrefs()
        Logger.d("LoraRepo", "Updated: $id")
    }

    override suspend fun delete(id: String) {
        _adapters.value = _adapters.value.filter { it.id != id }
        saveToPrefs()

        // If this was the active LoRA, clear the active ID
        val activeId = preferencesDataSource.getString("active_lora_id")
        if (activeId == id) {
            preferencesDataSource.remove("active_lora_id")
        }
        Logger.d("LoraRepo", "Deleted: $id")
    }

    override suspend fun getActive(): LoraAdapter? {
        val activeId = preferencesDataSource.getString("active_lora_id")
        if (activeId.isEmpty()) return null
        return _adapters.value.find { it.id == activeId }
    }

    override suspend fun setActive(id: String?) {
        if (id != null) {
            preferencesDataSource.putString("active_lora_id", id)
        } else {
            preferencesDataSource.remove("active_lora_id")
        }
        Logger.d("LoraRepo", "Set active: $id")
    }
}