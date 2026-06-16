package com.habibi.financeslm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoraAdapter(
    val id: String,
    val name: String,
    val instructionText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val baseModelId: String? = null,
    val filePath: String? = null
)