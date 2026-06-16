package com.habibi.financeslm.inference

import kotlinx.serialization.Serializable

@Serializable
data class LlamaConfig(
    val modelPath: String = "",
    val contextSize: Int = 2048,
    val batchSize: Int = 512,
    val threadCount: Int = 4,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val seed: Int = 42,
    val gpuLayers: Int = 0
)