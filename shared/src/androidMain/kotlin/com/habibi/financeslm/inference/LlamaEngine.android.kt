package com.habibi.financeslm.inference

import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Android stub LlamaEngine — will be replaced with JNI bridge to libllama.so.
 */
class AndroidLlamaEngine : LlamaEngine {
    private var loaded = false
    private var modelPath: String? = null

    override suspend fun loadModel(path: String, config: LlamaConfig): Boolean {
        Logger.d("LlamaEngine", "loadModel(stub-android): $path")
        loaded = true
        modelPath = path
        return true
    }

    override suspend fun unloadModel() {
        Logger.d("LlamaEngine", "unloadModel(stub-android)")
        loaded = false
        modelPath = null
    }

    override suspend fun isLoaded(): Boolean = loaded

    override suspend fun infer(prompt: String, params: InferenceParams): Flow<String> = flow {
        emit("[Android stub inference] Processing: ${prompt.take(50)}...")
        emit("\n")
        emit("Stub result. Real implementation will use JNI → libllama.so.")
    }

    override suspend fun tokenize(text: String): List<Int> {
        return text.map { it.code }
    }

    override suspend fun applyLora(loraPath: String): Boolean {
        Logger.d("LlamaEngine", "applyLora(stub-android): $loraPath")
        return true
    }

    override fun getLoadedModelPath(): String? = modelPath
}

actual fun createLlamaEngine(): LlamaEngine = AndroidLlamaEngine()