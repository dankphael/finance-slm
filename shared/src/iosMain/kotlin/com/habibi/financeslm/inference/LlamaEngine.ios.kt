package com.habibi.financeslm.inference

import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * iOS stub LlamaEngine — will be replaced with cinterop to libllama.a.
 */
class IosLlamaEngine : LlamaEngine {
    private var loaded = false
    private var modelPath: String? = null

    override suspend fun loadModel(path: String, config: LlamaConfig): Boolean {
        Logger.d("LlamaEngine", "loadModel(stub-ios): $path")
        loaded = true
        modelPath = path
        return true
    }

    override suspend fun unloadModel() {
        Logger.d("LlamaEngine", "unloadModel(stub-ios)")
        loaded = false
        modelPath = null
    }

    override suspend fun isLoaded(): Boolean = loaded

    override suspend fun infer(prompt: String, params: InferenceParams): Flow<String> = flow {
        emit("[iOS stub inference] Processing: ${prompt.take(50)}...")
        emit("\n")
        emit("Stub result. Real implementation will use cinterop → libllama.a.")
    }

    override suspend fun tokenize(text: String): List<Int> {
        return text.map { it.code }
    }

    override suspend fun applyLora(loraPath: String): Boolean {
        Logger.d("LlamaEngine", "applyLora(stub-ios): $loraPath")
        return true
    }

    override fun getLoadedModelPath(): String? = modelPath
}

actual fun createLlamaEngine(): LlamaEngine = IosLlamaEngine()