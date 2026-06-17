package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.prompt.PromptBuilder
import kotlinx.coroutines.flow.Flow

class GenerateInsightUseCase(
    private val inferenceRepository: InferenceRepository,
    private val screenDataRepository: ScreenDataRepository,
    private val modelRepository: ModelRepository,
    private val promptBuilder: PromptBuilder
) {
    suspend fun generateFromLatestScreen(loraInstruction: String? = null): Flow<String> {
        val recentData = screenDataRepository.getRecent(maxCount = 10)
        val selectedModel = modelRepository.getSelectedModel()
            ?: error("No model selected")
        val screenData = recentData.firstOrNull()
            ?: error("No screen data available")
        return generate(screenData, selectedModel.downloadedPath!!, selectedModel.chatTemplate, loraInstruction)
    }

    suspend fun generate(screenData: ScreenData, modelPath: String, chatTemplate: String = "qwen", loraInstruction: String? = null): Flow<String> {
        val prompt = promptBuilder.build(screenData, loraInstruction, chatTemplate)
        return inferenceRepository.generate(
            prompt = prompt,
            modelPath = modelPath,
            loraPath = null
        )
    }

    fun getInsights(): Flow<List<FinanceInsight>> {
        return inferenceRepository.getInsights()
    }
}