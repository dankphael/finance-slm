package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.platform.FileSystem as PlatformFileSystem
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Real ModelStorageDataSource — persists ModelInfo metadata as JSON files
 * alongside downloaded .gguf blobs on the platform filesystem.
 *
 * Metadata files are stored as <modelsDir>/{modelId}.metadata.json.
 * Model binary files are stored as <modelsDir>/{modelId}.gguf.
 */
class ModelStorageDataSource(
    private val platformFs: PlatformFileSystem,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())

    private val modelsDir: String get() = platformFs.getModelsDir()
    private val okioFs: FileSystem get() = FileSystem.SYSTEM

    /**
     * Scans the models directory for *.metadata.json files, parses each into
     * [ModelInfo], and returns the result as a [Flow] that emits once and completes.
     */
    fun getDownloadedModels(): Flow<List<ModelInfo>> {
        val modelsDirPath = modelsDir.toPath()
        val results = mutableListOf<ModelInfo>()

        if (!okioFs.exists(modelsDirPath)) {
            Logger.d("ModelStorageDS", "Models directory does not exist: $modelsDir")
            return MutableStateFlow(emptyList<ModelInfo>()).asStateFlow()
        }

        try {
            okioFs.list(modelsDirPath).forEach { path ->
                val name = path.name
                if (name.endsWith(".metadata.json")) {
                    try {
                        val content = okioFs.read(path) {
                            readUtf8()
                        }
                        val modelInfo = json.decodeFromString<ModelInfo>(content)
                        // Ensure downloadedPath points to the actual .gguf file
                        val ggufPath = modelsDirPath / "${modelInfo.id}.gguf"
                        if (okioFs.exists(ggufPath)) {
                            results.add(modelInfo.copy(downloadedPath = ggufPath.toString()))
                        } else {
                            Logger.w("ModelStorageDS", "Orphaned metadata (no .gguf): ${path.name}")
                        }
                    } catch (e: Exception) {
                        Logger.e("ModelStorageDS", "Failed to parse metadata: ${path.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("ModelStorageDS", "Failed to list models directory", e)
        }

        _models.value = results
        return _models.asStateFlow()
    }

    /**
     * Persists a [ModelInfo] as a JSON metadata file at
     * <modelsDir>/{model.id}.metadata.json.
     */
    suspend fun saveModel(model: ModelInfo) {
        val metaPath = "${modelsDir}/${model.id}.metadata.json".toPath()
        try {
            okioFs.write(metaPath) {
                writeUtf8(json.encodeToString(model))
            }
            Logger.d("ModelStorageDS", "Saved metadata: ${model.id}")

            // Refresh the in-memory list to include the new model
            getDownloadedModels()
        } catch (e: Exception) {
            Logger.e("ModelStorageDS", "Failed to save model metadata: ${model.id}", e)
            throw e
        }
    }

    /**
     * Deletes both the .gguf binary and the .metadata.json file for the given
     * [modelId]. Returns silently even if one or both files do not exist.
     */
    suspend fun deleteModel(modelId: String) {
        val ggufPath = "${modelsDir}/${modelId}.gguf".toPath()
        val metaPath = "${modelsDir}/${modelId}.metadata.json".toPath()

        var deletedAny = false

        try {
            if (okioFs.exists(ggufPath)) {
                okioFs.delete(ggufPath)
                deletedAny = true
                Logger.d("ModelStorageDS", "Deleted .gguf: ${ggufPath.name}")
            }
        } catch (e: Exception) {
            Logger.e("ModelStorageDS", "Failed to delete .gguf: ${modelId}", e)
        }

        try {
            if (okioFs.exists(metaPath)) {
                okioFs.delete(metaPath)
                deletedAny = true
                Logger.d("ModelStorageDS", "Deleted metadata: ${metaPath.name}")
            }
        } catch (e: Exception) {
            Logger.e("ModelStorageDS", "Failed to delete metadata: ${modelId}", e)
        }

        if (deletedAny) {
            // Rebuild the downloaded list
            getDownloadedModels()
        }
    }

    /**
     * Returns true if the .gguf binary for [modelId] exists and has a file size
     * greater than zero.
     */
    fun modelExists(modelId: String): Boolean {
        val path = getModelPath(modelId)
        val okioPath = path.toPath()
        return if (okioFs.exists(okioPath)) {
            val size = okioFs.metadata(okioPath).size
            size != null && size > 0L
        } else {
            false
        }
    }

    /**
     * Returns the absolute path to the .gguf file for [modelId],
     * regardless of whether it exists. Use [modelExists] to check existence.
     */
    fun getModelPath(modelId: String): String = "${modelsDir}/${modelId}.gguf"
}