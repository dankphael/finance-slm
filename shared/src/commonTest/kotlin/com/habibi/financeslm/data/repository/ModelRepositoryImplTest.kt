package com.habibi.financeslm.data.repository

import com.habibi.financeslm.data.datasource.ModelDownloadDataSource
import com.habibi.financeslm.data.datasource.ModelStorageDataSource
import com.habibi.financeslm.data.datasource.PreferencesDataSource
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.platform.FileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------
// Handwritten fakes
// ---------------------------------------------------------------

private class FakeDownloadDataSource : ModelDownloadDataSource {
    override suspend fun download(
        url: String,
        destinationPath: String,
        expectedSha256: String
    ): Flow<DownloadState> = MutableStateFlow(DownloadState.Done)
}

private class FakeStorageDataSource : ModelStorageDataSource {
    private val stored = mutableListOf<ModelInfo>()

    override suspend fun getDownloadedModels(): Flow<List<ModelInfo>> = flowOf(stored.toList())
    override suspend fun saveModel(model: ModelInfo) { stored.add(model) }
    override suspend fun deleteModel(modelId: String) { stored.removeAll { it.id == modelId } }
    override fun getModelPath(modelId: String): String = "/data/models/$modelId.gguf"
}

private class FakePreferencesDataSource : PreferencesDataSource {
    private val prefs = mutableMapOf<String, String>()

    override fun getString(key: String): String = prefs[key] ?: ""
    override fun putString(key: String, value: String) { prefs[key] = value }
    override fun remove(key: String) { prefs.remove(key) }
}

private class FakeFileSystem : FileSystem {
    override fun getDataDir(): String = "/data"
    override fun exists(path: String): Boolean = true
    override fun deleteFile(path: String) {}
}

private class FakeDownloadEnqueuer : DownloadEnqueuer {
    override fun enqueueDownload(
        url: String,
        destinationPath: String,
        expectedSha256: String,
        modelId: String
    ): Flow<DownloadState> = flowOf(DownloadState.Done)

    override suspend fun cancelDownload(modelId: String) {}
}

// ---------------------------------------------------------------
// Tests
// ---------------------------------------------------------------

class ModelRepositoryImplTest {

    private val validCatalogJson = """
        {
            "version": 1,
            "models": [
                {
                    "id": "finance-llama-3b",
                    "name": "Finance Llama 3B",
                    "description": "3B parameter finance model",
                    "url": "https://example.com/finance-llama-3b.gguf",
                    "sizeBytes": 2000000000,
                    "sha256": "abc123def456",
                    "minRamMb": 4096,
                    "quantization": "Q4_K_M",
                    "contextSize": 4096,
                    "chatTemplate": "llama"
                },
                {
                    "id": "finance-qwen-1.5b",
                    "name": "Finance Qwen 1.5B",
                    "description": "1.5B parameter finance model",
                    "url": "https://example.com/finance-qwen-1.5b.gguf",
                    "sizeBytes": 1000000000,
                    "sha256": "789ghi012jkl",
                    "minRamMb": 2048,
                    "quantization": "Q4_K_M",
                    "contextSize": 8192,
                    "chatTemplate": "qwen"
                }
            ]
        }
    """.trimIndent()

    @Test
    fun `loadCatalogFromJson parses valid JSON`() {
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson(validCatalogJson)

        val catalog = repo.getCatalog().first()
        assertEquals(2, catalog.size)
        assertEquals("finance-llama-3b", catalog[0].id)
        assertEquals("Finance Llama 3B", catalog[0].name)
        assertEquals("finance-qwen-1.5b", catalog[1].id)
        assertEquals("Finance Qwen 1.5B", catalog[1].name)
    }

    @Test
    fun `loadCatalogFromJson with invalid JSON returns empty`() {
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson("not valid json")

        val catalog = repo.getCatalog().first()
        assertTrue(catalog.isEmpty())
    }

    @Test
    fun `loadCatalogFromJson with empty model list`() {
        val json = """{"version": 1, "models": []}"""
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson(json)

        val catalog = repo.getCatalog().first()
        assertTrue(catalog.isEmpty())
    }

    @Test
    fun `getCatalog returns loaded catalog`() {
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson(validCatalogJson)

        val firstGet = repo.getCatalog().first()
        assertEquals(2, firstGet.size)

        // Second call returns same data
        val secondGet = repo.getCatalog().first()
        assertEquals(firstGet, secondGet)
    }

    @Test
    fun `catalog is empty before loading`() = runTest {
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        val catalog = repo.getCatalog().first()
        assertTrue(catalog.isEmpty())
    }

    @Test
    fun `selectModel and getSelectedModel work correctly`() = runTest {
        val prefs = FakePreferencesDataSource()
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = prefs,
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson(validCatalogJson)

        // Before selection, no model is selected (nothing in downloaded list)
        assertNull(repo.getSelectedModel())

        // Select model
        repo.selectModel("finance-llama-3b")

        // Since it's not in downloaded models, and it's in catalog, getSelectedModel
        // searches downloaded first, then catalog.
        // With empty downloaded list, it should find it in catalog.
        // But wait - getSelectedModel searches _downloaded.value first, then _catalog.value
        val selected = repo.getSelectedModel()
        assertEquals("finance-llama-3b", selected?.id)
        assertEquals("Finance Llama 3B", selected?.name)
    }

    @Test
    fun `selectModel persists via preferences`() = runTest {
        val prefs = FakePreferencesDataSource()
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = prefs,
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.selectModel("finance-qwen-1.5b")
        assertEquals("finance-qwen-1.5b", prefs.getString("selected_model_id"))
    }

    @Test
    fun `selectModel returns null for unknown model`() = runTest {
        val repo = ModelRepositoryImpl(
            downloadDataSource = FakeDownloadDataSource(),
            storageDataSource = FakeStorageDataSource(),
            preferencesDataSource = FakePreferencesDataSource(),
            fileSystem = FakeFileSystem(),
            downloadEnqueuer = FakeDownloadEnqueuer()
        )

        repo.loadCatalogFromJson(validCatalogJson)
        repo.selectModel("non-existent-model-id")

        // Not in downloaded or catalog
        assertNull(repo.getSelectedModel())
    }
}