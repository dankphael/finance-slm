package com.habibi.financeslm.data.mapper

import com.habibi.financeslm.domain.model.ModelInfo

/**
 * Stub ModelMapper for compilation.
 * Real implementation will handle DTO <-> Domain mapping.
 */
object ModelMapper {
    fun fromJson(json: String): List<ModelInfo> {
        return emptyList() // Stub
    }

    fun toJson(models: List<ModelInfo>): String {
        return "[]" // Stub
    }
}