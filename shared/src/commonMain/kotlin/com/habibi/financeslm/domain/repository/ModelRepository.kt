package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun getCatalog(): Flow<List<ModelInfo>>
    suspend fun refreshCatalog()
    suspend fun downloadModel(modelId: String): Flow<DownloadState>
    suspend fun deleteModel(modelId: String)
    fun getDownloadedModels(): Flow<List<ModelInfo>>
    suspend fun getSelectedModel(): ModelInfo?
    suspend fun selectModel(modelId: String)
    fun getDownloadProgress(modelId: String): Flow<DownloadState>

    /**
     * Load the model catalog from a JSON string (read from bundled assets).
     * This is an Android-specific concern exposed here for cross-module access.
     */
    fun loadCatalogFromJson(jsonContent: String)
}