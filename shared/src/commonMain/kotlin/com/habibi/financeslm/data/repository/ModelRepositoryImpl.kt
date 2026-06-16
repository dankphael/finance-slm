package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub ModelRepository for compilation.
 * Real implementation will use Ktor for downloads and SQLDelight for persistence.
 */
class ModelRepositoryImpl : ModelRepository {
    private val _catalog = MutableStateFlow<List<ModelInfo>>(emptyList())
    private val _downloaded = MutableStateFlow<List<ModelInfo>>(emptyList())
    private var _selectedModelId: String? = null

    override fun getCatalog(): Flow<List<ModelInfo>> = _catalog.asStateFlow()

    override suspend fun refreshCatalog() {
        Logger.d("ModelRepo", "refreshCatalog(stub)")
    }

    override suspend fun downloadModel(modelId: String): Flow<DownloadState> {
        Logger.d("ModelRepo", "downloadModel(stub): $modelId")
        return MutableStateFlow(DownloadState.Idle).asStateFlow()
    }

    override suspend fun deleteModel(modelId: String) {
        Logger.d("ModelRepo", "deleteModel(stub): $modelId")
    }

    override fun getDownloadedModels(): Flow<List<ModelInfo>> = _downloaded.asStateFlow()

    override suspend fun getSelectedModel(): ModelInfo? {
        return _downloaded.value.find { it.id == _selectedModelId }
    }

    override suspend fun selectModel(modelId: String) {
        _selectedModelId = modelId
    }

    override fun getDownloadProgress(modelId: String): Flow<DownloadState> {
        return MutableStateFlow(DownloadState.Idle).asStateFlow()
    }
}