package com.habibi.financeslm.inference

import kotlinx.coroutines.flow.Flow

/**
 * LlamaEngine — platform-specific llama.cpp inference engine wrapper.
 * On Android: JNI bridge to libllama.so
 * On iOS: cinterop to libllama.a
 */
interface LlamaEngine {
    suspend fun loadModel(path: String, config: LlamaConfig): Boolean
    suspend fun unloadModel()
    suspend fun isLoaded(): Boolean
    suspend fun infer(prompt: String, params: InferenceParams): Flow<String>
    suspend fun tokenize(text: String): List<Int>
    suspend fun applyLora(loraPath: String): Boolean
    fun getLoadedModelPath(): String?
}