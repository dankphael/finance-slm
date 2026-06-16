package com.habibi.financeslm.inference

import kotlinx.serialization.Serializable

@Serializable
data class InferenceParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val stopTokens: List<String> = listOf("</s>", "<|im_end|>")
)