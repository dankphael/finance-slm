package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Stub ModelStorageDataSource for compilation.
 * Real implementation will serialize/deserialize models from the filesystem.
 */
class ModelStorageDataSource {
    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())

    fun getDownloadedModels(): Flow<List<ModelInfo>> = _models.asStateFlow()

    suspend fun saveModel(model: ModelInfo) {
        Logger.d("StorageDS", "saveModel(stub): ${model.id}")
    }

    suspend fun deleteModel(modelId: String) {
        Logger.d("StorageDS", "deleteModel(stub): $modelId")
    }

    fun modelExists(modelId: String): Boolean = false
}