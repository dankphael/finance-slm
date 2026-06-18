package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.FinanceInsight
import kotlinx.coroutines.flow.Flow

interface InferenceRepository {
    suspend fun generate(prompt: String, modelPath: String, loraPath: String? = null): Flow<String>
    fun getInsights(): Flow<List<FinanceInsight>>
    suspend fun clearInsights()

    /** Load a model into memory (idempotent if already loaded). */
    suspend fun loadModel(modelPath: String): Boolean

    /** Unload the currently loaded model from memory. */
    suspend fun unloadModel()

    /** Whether a model is currently loaded in memory. */
    suspend fun isModelLoaded(): Boolean
}