package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.repository.ModelRepository

class DeleteModelUseCase(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(modelId: String) {
        modelRepository.deleteModel(modelId)
    }
}