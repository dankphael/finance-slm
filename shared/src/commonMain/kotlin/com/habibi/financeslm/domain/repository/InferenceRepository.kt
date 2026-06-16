package com.habibi.financeslm.domain.repository

import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.ScreenData
import kotlinx.coroutines.flow.Flow

interface InferenceRepository {
    suspend fun generate(prompt: String, modelPath: String, loraPath: String? = null): Flow<String>
    suspend fun generateInsight(screenData: ScreenData, loraInstruction: String? = null): Flow<String>
    fun getInsights(): Flow<List<FinanceInsight>>
    suspend fun clearInsights()
}