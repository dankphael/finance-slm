package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.InsightCategory
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.inference.InferenceParams
import com.habibi.financeslm.inference.LlamaConfig
import com.habibi.financeslm.inference.LlamaEngine
import com.habibi.financeslm.prompt.PromptBuilder
import com.habibi.financeslm.util.Logger
import com.habibi.financeslm.util.SingleThreadDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Real InferenceRepository implementation.
 *
 * Wires to [LlamaEngine] (JNI bridge to llama.cpp) for actual model inference.
 * Uses a single-thread dispatcher for ALL llama.cpp calls (Pat's gotcha G4).
 */
class InferenceRepositoryImpl(
    private val llamaEngine: LlamaEngine,
    private val promptBuilder: PromptBuilder
) : InferenceRepository {

    /**
     * Single-thread dispatcher for all llama.cpp calls.
     * This ensures thread safety — llama.cpp is NOT thread-safe.
     * Pat's gotcha G4: MUST use a single-thread dispatcher for ALL inference calls.
     */
    private val inferenceDispatcher = SingleThreadDispatcher.dispatcher

    private val _insights = MutableStateFlow<List<FinanceInsight>>(emptyList())
    private var idCounter = 1L

    override suspend fun generate(prompt: String, modelPath: String, loraPath: String?): Flow<String> = flow {
        val currentId = idCounter++

        Logger.d("InferenceRepo", "generate() start — model=$modelPath, prompt_len=${prompt.length}")

        // Build the inference context
        val config = LlamaConfig(
            contextSize = 2048,
            batchSize = 512,
            threadCount = 4,
            gpuLayers = 0
        )

        val params = InferenceParams(
            maxTokens = 512,
            temperature = 0.7f,
            topP = 0.9f,
            topK = 40,
            repeatPenalty = 1.1f
        )

        var loaded = false
        try {
            // 1. Load the model (on single-thread dispatcher)
            loaded = withContext(inferenceDispatcher) {
                llamaEngine.loadModel(modelPath, config)
            }
            if (!loaded) {
                emit("[error] Failed to load model: $modelPath")
                return@flow
            }
            Logger.d("InferenceRepo", "Model loaded: $modelPath")

            // 2. Apply LoRA if provided
            if (!loraPath.isNullOrEmpty()) {
                val loraApplied = withContext(inferenceDispatcher) {
                    llamaEngine.applyLora(loraPath)
                }
                if (!loraApplied) {
                    Logger.w("InferenceRepo", "LoRA application returned false (may be unsupported): $loraPath")
                }
            }

            // 3. Run inference — collect token stream
            val tokenFlow = withContext(inferenceDispatcher) {
                llamaEngine.infer(prompt, params)
            }

            val fullOutput = StringBuilder()
            tokenFlow.collect { token ->
                fullOutput.append(token)
                emit(token)
            }

            val resultText = fullOutput.toString()
            Logger.d("InferenceRepo", "Inference complete: ${resultText.length} chars")

            // 4. Create a FinanceInsight from the result
            val insight = FinanceInsight(
                id = "insight_$currentId",
                title = extractTitle(resultText),
                summary = resultText.take(200),
                detailText = resultText,
                category = categorizeResult(resultText),
                sourceApp = null,
                timestamp = System.currentTimeMillis(),
                loraAdapterId = null
            )
            _insights.value = _insights.value + insight
            Logger.d("InferenceRepo", "Insight created: ${insight.id}")

        } catch (e: Exception) {
            Logger.e("InferenceRepo", "Inference error", e)
            emit("[error] ${e.message}")
        } finally {
            // 5. Always unload model (try/finally ensures this)
            if (loaded) {
                try {
                    withContext(inferenceDispatcher) {
                        llamaEngine.unloadModel()
                    }
                    Logger.d("InferenceRepo", "Model unloaded")
                } catch (e: Exception) {
                    Logger.e("InferenceRepo", "Error unloading model", e)
                }
            }
        }
    }

    override suspend fun generateInsight(screenData: ScreenData, loraInstruction: String?): Flow<String> {
        val prompt = if (loraInstruction != null) {
            promptBuilder.build(screenData, loraInstruction)
        } else {
            promptBuilder.build(screenData)
        }

        return generate(prompt, "", loraPath = if (loraInstruction != null) "lora" else null)
    }

    override fun getInsights(): Flow<List<FinanceInsight>> = _insights.asStateFlow()

    override suspend fun clearInsights() {
        _insights.value = emptyList()
        Logger.d("InferenceRepo", "Cleared insights")
    }

    /**
     * Extract a title from the generated text.
     */
    private fun extractTitle(text: String): String {
        // Try to use the first line or first meaningful sentence
        val firstLine = text.lines().firstOrNull { it.isNotBlank() }
        if (firstLine != null && firstLine.length <= 80) return firstLine.trim().removePrefix("#").trim()

        val firstSentence = text.split(".", "!", "?").firstOrNull { it.isNotBlank() }
        if (firstSentence != null) return firstSentence.trim().take(80)

        return "Financial Insight #${idCounter - 1}"
    }

    /**
     * Categorize the generated result based on content keywords.
     */
    private fun categorizeResult(text: String): InsightCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("spend") || lower.contains("expense") || lower.contains("transaction") -> InsightCategory.SPENDING
            lower.contains("savings") || lower.contains("save") || lower.contains("emergency fund") -> InsightCategory.SAVINGS
            lower.contains("invest") || lower.contains("stock") || lower.contains("cpf") ||
                lower.contains("ssb") || lower.contains("t-bill") || lower.contains("srs") -> InsightCategory.INVESTMENT
            lower.contains("budget") || lower.contains("limit") || lower.contains("plan") -> InsightCategory.BUDGET
            else -> InsightCategory.GENERAL
        }
    }
}