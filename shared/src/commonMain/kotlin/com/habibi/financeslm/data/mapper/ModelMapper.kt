package com.habibi.financeslm.data.mapper

import com.habibi.financeslm.domain.model.ModelInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * DTO for the model catalog JSON structure.
 */
@Serializable
data class ModelCatalogDto(
    val version: Int,
    val models: List<ModelInfo>
)

/**
 * Real ModelMapper — parses the model catalog JSON into domain [ModelInfo] objects.
 */
object ModelMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(jsonContent: String): List<ModelInfo> {
        return try {
            val catalog = json.decodeFromString<ModelCatalogDto>(jsonContent)
            catalog.models
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(models: List<ModelInfo>): String {
        return json.encodeToString(ModelCatalogDto.serializer(), ModelCatalogDto(version = 1, models = models))
    }
}