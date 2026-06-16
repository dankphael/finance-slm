package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.model.LoraAdapter
import com.habibi.financeslm.domain.repository.LoraRepository
import kotlinx.coroutines.flow.Flow

class ManageLoraUseCase(
    private val loraRepository: LoraRepository
) {
    fun getAll(): Flow<List<LoraAdapter>> = loraRepository.getAll()

    suspend fun getById(id: String): LoraAdapter? = loraRepository.getById(id)

    suspend fun create(name: String, instructionText: String): LoraAdapter {
        return loraRepository.create(name, instructionText)
    }

    suspend fun update(id: String, name: String, instructionText: String) {
        loraRepository.update(id, name, instructionText)
    }

    suspend fun delete(id: String) {
        loraRepository.delete(id)
    }

    suspend fun getActive(): LoraAdapter? = loraRepository.getActive()

    suspend fun setActive(id: String?) {
        loraRepository.setActive(id)
    }
}