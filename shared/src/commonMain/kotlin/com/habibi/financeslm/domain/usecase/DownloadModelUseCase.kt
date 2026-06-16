package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow

class DownloadModelUseCase(
    private val modelRepository: ModelRepository
) {
    operator fun invoke(modelId: String): Flow<DownloadState> {
        return modelRepository.getDownloadProgress(modelId)
    }

    suspend fun startDownload(modelId: String): Flow<DownloadState> {
        return modelRepository.downloadModel(modelId)
    }

    suspend fun deleteModel(modelId: String) {
        modelRepository.deleteModel(modelId)
    }

    suspend fun selectModel(modelId: String) {
        modelRepository.selectModel(modelId)
    }

    fun getAvailableModels(): Flow<List<ModelInfo>> {
        return modelRepository.getCatalog()
    }

    fun getDownloadedModels(): Flow<List<ModelInfo>> {
        return modelRepository.getDownloadedModels()
    }
}