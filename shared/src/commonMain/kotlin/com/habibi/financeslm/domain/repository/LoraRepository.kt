package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.LoraAdapter
import kotlinx.coroutines.flow.Flow

interface LoraRepository {
    fun getAll(): Flow<List<LoraAdapter>>
    suspend fun getById(id: String): LoraAdapter?
    suspend fun create(name: String, instructionText: String): LoraAdapter
    suspend fun update(id: String, name: String, instructionText: String)
    suspend fun delete(id: String)
    suspend fun getActive(): LoraAdapter?
    suspend fun setActive(id: String?)
}