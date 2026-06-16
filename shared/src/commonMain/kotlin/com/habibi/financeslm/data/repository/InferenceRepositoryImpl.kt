package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.InsightCategory
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Stub InferenceRepository for compilation.
 * Real implementation will call LlamaEngine via native bindings.
 */
class InferenceRepositoryImpl : InferenceRepository {
    private val _insights = MutableStateFlow<List<FinanceInsight>>(emptyList())
    private var idCounter = 1L

    override suspend fun generate(prompt: String, modelPath: String, loraPath: String?): Flow<String> = flow {
        emit("[Stub inference] Prompt: ${prompt.take(50)}...")
        emit("\n")
        emit("This is a placeholder. Real inference requires llama.cpp.")
    }

    override suspend fun generateInsight(screenData: ScreenData, loraInstruction: String?): Flow<String> = flow {
        val now = idCounter++
        val insight = FinanceInsight(
            id = "stub_$now",
            title = "Sample Insight (Stub)",
            summary = "This is a placeholder insight. Real insights require a loaded model.",
            detailText = "Once llama.cpp is integrated and a model is downloaded, this will contain AI-generated financial tips.",
            category = InsightCategory.GENERAL,
            sourceApp = screenData.sourceApp,
            timestamp = now
        )
        _insights.value = _insights.value + insight
        emit(insight.summary)
    }

    override fun getInsights(): Flow<List<FinanceInsight>> = _insights.asStateFlow()

    override suspend fun clearInsights() {
        _insights.value = emptyList()
        Logger.d("InferenceRepo", "Cleared insights")
    }
}