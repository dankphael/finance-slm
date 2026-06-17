package com.habibi.financeslm.data.repository

import com.habibi.financeslm.db.FinanceSlmDatabase
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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

/**
 * Real InferenceRepository implementation with SQLDelight persistence.
 *
 * Wires to [LlamaEngine] (JNI bridge to llama.cpp) for actual model inference.
 * Uses a single-thread dispatcher for ALL llama.cpp calls (Pat's gotcha G4).
 * Insights are persisted to SQLDelight database instead of in-memory only.
 */
class InferenceRepositoryImpl(
    private val llamaEngine: LlamaEngine,
    private val promptBuilder: PromptBuilder,
    private val database: FinanceSlmDatabase
) : InferenceRepository {

    private var idCounter = 1L

    /**
     * Single-thread dispatcher for all llama.cpp calls.
     * This ensures thread safety — llama.cpp is NOT thread-safe.
     * Pat's gotcha G4: MUST use a single-thread dispatcher for ALL inference calls.
     */
    private val inferenceDispatcher = SingleThreadDispatcher.dispatcher

    /** Tracks the currently loaded model path — null means nothing loaded. */
    private var loadedModelPath: String? = null

    override suspend fun generate(prompt: String, modelPath: String, loraPath: String?): Flow<String> {
        val currentId = idCounter++

        Logger.d("InferenceRepo", "generate() start — model=$modelPath, prompt_len=${prompt.length}")

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

        var isNewLoad = false
        return callbackFlow {
            try {
                // 1. Load model only if not already the active model
                if (loadedModelPath != modelPath) {
                    isNewLoad = withContext(inferenceDispatcher) {
                        if (loadedModelPath != null) {
                            llamaEngine.unloadModel()
                            loadedModelPath = null
                        }
                        val loaded = llamaEngine.loadModel(modelPath, config)
                        if (loaded) loadedModelPath = modelPath
                        loaded
                    }
                }
                if (!isNewLoad && loadedModelPath != modelPath) {
                    trySend("[error] Failed to load model: $modelPath")
                    close()
                    return@callbackFlow
                }
                Logger.d("InferenceRepo", "Model ready: $modelPath")

                // 2. Apply LoRA if provided
                if (!loraPath.isNullOrEmpty()) {
                    val loraApplied = withContext(inferenceDispatcher) {
                        llamaEngine.applyLora(loraPath)
                    }
                    if (!loraApplied) {
                        Logger.w("InferenceRepo", "LoRA application returned false: $loraPath")
                    }
                }

                // 3. Run inference — stream tokens in real-time via callbackFlow
                val fullOutput = StringBuilder()
                withContext(inferenceDispatcher) {
                    llamaEngine.inferStreaming(prompt, params).collect { token ->
                        fullOutput.append(token)
                        trySend(token)
                    }
                }

                Logger.d("InferenceRepo", "Inference complete: ${fullOutput.length} chars")

                // 4. Create and persist a FinanceInsight
                val resultText = fullOutput.toString()
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
                database.financeInsightQueries.insertOrReplace(
                    insight.id, insight.title, insight.summary, insight.detailText,
                    insight.category.name, insight.sourceApp, insight.timestamp, insight.loraAdapterId
                )
                Logger.d("InferenceRepo", "Insight persisted: ${insight.id}")

            } catch (e: Exception) {
                Logger.e("InferenceRepo", "Inference error", e)
                trySend("[error] ${e.message}")
            } catch (e: Throwable) {
                // Catch native crash exceptions (RuntimeException thrown from signal handler)
                Logger.e("InferenceRepo", "Native crash caught: ${e.message}")
                trySend("[error] Native inference engine error: ${e.message}")
            } finally {
                // 5. Only unload if THIS call loaded the model (not pre-loaded via loadModel())
                if (isNewLoad) {
                    try {
                        withContext(inferenceDispatcher) { llamaEngine.unloadModel() }
                        loadedModelPath = null
                        Logger.d("InferenceRepo", "Model unloaded")
                    } catch (e: Exception) {
                        Logger.e("InferenceRepo", "Error unloading model", e)
                    }
                }
                close()
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

    override fun getInsights(): Flow<List<FinanceInsight>> {
        return database.financeInsightQueries.getAll()
            .asFlow()
            .mapToList(kotlinx.coroutines.Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    FinanceInsight(
                        id = row.id,
                        title = row.title,
                        summary = row.summary,
                        detailText = row.detail_text,
                        category = try { InsightCategory.valueOf(row.category) } catch (_: Exception) { InsightCategory.GENERAL },
                        sourceApp = row.source_app,
                        timestamp = row.timestamp,
                        loraAdapterId = row.lora_adapter_id
                    )
                }
            }
    }

    override suspend fun clearInsights() {
        database.financeInsightQueries.deleteAll()
        Logger.d("InferenceRepo", "Cleared insights")
    }

    override suspend fun loadModel(modelPath: String): Boolean = withContext(inferenceDispatcher) {
        if (loadedModelPath == modelPath) {
            Logger.d("InferenceRepo", "Model already loaded: $modelPath")
            return@withContext true
        }
        if (loadedModelPath != null) {
            llamaEngine.unloadModel()
            loadedModelPath = null
        }
        val config = LlamaConfig(contextSize = 2048, batchSize = 512, threadCount = 4, gpuLayers = 0)
        val loaded = llamaEngine.loadModel(modelPath, config)
        if (loaded) {
            loadedModelPath = modelPath
            Logger.d("InferenceRepo", "Model loaded: $modelPath")
        }
        loaded
    }

    override suspend fun unloadModel() = withContext(inferenceDispatcher) {
        if (loadedModelPath != null) {
            llamaEngine.unloadModel()
            loadedModelPath = null
            Logger.d("InferenceRepo", "Model unloaded via unloadModel()")
        }
    }

    override suspend fun isModelLoaded(): Boolean = loadedModelPath != null

    private fun extractTitle(text: String): String {
        val firstLine = text.lines().firstOrNull { it.isNotBlank() }
        if (firstLine != null && firstLine.length <= 80) return firstLine.trim().removePrefix("#").trim()
        val firstSentence = text.split(".", "!", "?").firstOrNull { it.isNotBlank() }
        if (firstSentence != null) return firstSentence.trim().take(80)
        return "Financial Insight #${idCounter - 1}"
    }

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
