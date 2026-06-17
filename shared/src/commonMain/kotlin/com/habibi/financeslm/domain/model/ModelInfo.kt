package com.habibi.financeslm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val minRamMb: Int,
    val quantization: String,
    val contextSize: Int,
    val chatTemplate: String = "qwen",
    val downloadedPath: String? = null
)