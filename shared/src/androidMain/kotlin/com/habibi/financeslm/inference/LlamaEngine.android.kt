package com.habibi.financeslm.inference

import com.habibi.financeslm.util.Logger
import com.habibi.financeslm.util.SingleThreadDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Android LlamaEngine — JNI bridge to libllama.so.
 *
 * Native methods are registered via JNI_OnLoad + RegisterNatives in llama_jni.cpp
 * (NOT name mangling), so the method names are independent of the class/package name.
 */
class LlamaEngineAndroid : LlamaEngine {

    private var loadedModelPath: String? = null

    // ── JNI extern declarations ───────────────────────────────────────────
    // These map to functions registered in llama_jni.cpp via JNI_OnLoad + RegisterNatives.

    private external fun nativeLoadModel(
        path: String,
        contextSize: Int,
        batchSize: Int,
        threadCount: Int,
        gpuLayers: Int
    ): Boolean

    private external fun nativeFreeModel()

    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String

    private external fun nativeTokenize(text: String): IntArray?

    private external fun nativeApplyLora(loraPath: String): Boolean

    private external fun nativeIsLoaded(): Boolean

    // ── Companion: ensure native lib is loaded ────────────────────────────
    companion object {
        init {
            try {
                System.loadLibrary("llamajni")
                Logger.d("LlamaEngineAndroid", "libllamajni.so loaded")
            } catch (e: UnsatisfiedLinkError) {
                Logger.d("LlamaEngineAndroid", "libllamajni.so not found, JNI will not be available: ${e.message}")
            }
        }
    }

    // ── LlamaEngine interface implementation ──────────────────────────────

    override suspend fun loadModel(path: String, config: LlamaConfig): Boolean = withContext(SingleThreadDispatcher.dispatcher) {
        Logger.d("LlamaEngineAndroid", "loadModel: $path")
        val result = nativeLoadModel(
            path = path,
            contextSize = config.contextSize,
            batchSize = config.batchSize,
            threadCount = config.threadCount,
            gpuLayers = config.gpuLayers
        )
        if (result) {
            loadedModelPath = path
        }
        result
    }

    override suspend fun unloadModel() = withContext(SingleThreadDispatcher.dispatcher) {
        Logger.d("LlamaEngineAndroid", "unloadModel")
        nativeFreeModel()
        loadedModelPath = null
    }

    override suspend fun isLoaded(): Boolean = nativeIsLoaded()

    override suspend fun infer(prompt: String, params: InferenceParams): Flow<String> = flow {
        Logger.d("LlamaEngineAndroid", "infer: prompt_len=${prompt.length}, max_tokens=${params.maxTokens}")

        val result = withContext(SingleThreadDispatcher.dispatcher) {
            nativeGenerate(
                prompt = prompt,
                maxTokens = params.maxTokens,
                temperature = params.temperature,
                topP = params.topP,
                topK = params.topK,
                repeatPenalty = params.repeatPenalty
            )
        }

        if (result.isNotEmpty()) {
            emit(result)
        } else {
            emit("[error] Inference returned empty result")
        }
    }

    override suspend fun tokenize(text: String): List<Int> = withContext(SingleThreadDispatcher.dispatcher) {
        val arr = nativeTokenize(text)
        arr?.toList() ?: emptyList()
    }

    override suspend fun applyLora(loraPath: String): Boolean = withContext(SingleThreadDispatcher.dispatcher) {
        Logger.d("LlamaEngineAndroid", "applyLora: $loraPath")
        nativeApplyLora(loraPath)
    }

    override fun getLoadedModelPath(): String? = loadedModelPath
}

actual fun createLlamaEngine(): LlamaEngine = LlamaEngineAndroid()