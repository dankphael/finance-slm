package com.habibi.financeslm.domain.usecase

import com.habibi.financeslm.domain.model.*
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.prompt.PromptBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// ---------------------------------------------------------------
// Handwritten fakes
// ---------------------------------------------------------------

private class FakeInferenceRepository(
    private val generateResult: Flow<String> = flow { emit("insight text") },
    private val insightsResult: Flow<List<FinanceInsight>> = flow { emit(emptyList()) }
) : InferenceRepository {
    var lastPrompt: String? = null
    var lastModelPath: String? = null
    var generateCallCount = 0

    override suspend fun generate(prompt: String, modelPath: String, loraPath: String?): Flow<String> {
        lastPrompt = prompt
        lastModelPath = modelPath
        generateCallCount++
        return generateResult
    }

    override suspend fun generateInsight(screenData: ScreenData, loraInstruction: String?): Flow<String> =
        flow { emit("") }
    override fun getInsights(): Flow<List<FinanceInsight>> = insightsResult
    override suspend fun clearInsights() {}
    override suspend fun loadModel(modelPath: String): Boolean = true
    override suspend fun unloadModel() {}
    override suspend fun isModelLoaded(): Boolean = true
}

private class FakeScreenDataRepository(
    private val data: List<ScreenData> = listOf(
        ScreenData(
            id = "screen-1",
            sourcePackage = "com.ocbc.mobile",
            sourceApp = "OCBC Digital",
            textContent = "Account balance SGD 12,000.00",
            timestamp = 1_700_000_000_000L
        )
    )
) : ScreenDataRepository {
    override fun observeScreenData(): Flow<ScreenData> =
        flow { emit(data.first()) }

    override suspend fun getRecent(maxCount: Int): List<ScreenData> = data.take(maxCount)

    override suspend fun clear() {}
}

private class FakeModelRepository(
    private val selected: ModelInfo? = ModelInfo(
        id = "model-1",
        name = "Finance Llama",
        description = "Fine-tuned finance model",
        url = "https://example.com/model.gguf",
        sizeBytes = 2_000_000_000,
        sha256 = "abc123",
        minRamMb = 4096,
        quantization = "Q4_K_M",
        contextSize = 4096,
        chatTemplate = "llama",
        downloadedPath = "/data/models/finance-llama.gguf"
    )
) : ModelRepository {
    override fun getCatalog(): Flow<List<ModelInfo>> = MutableStateFlow(emptyList())
    override suspend fun refreshCatalog() {}
    override suspend fun downloadModel(modelId: String): Flow<DownloadState> =
        flow { emit(DownloadState.Idle) }
    override suspend fun deleteModel(modelId: String) {}
    override fun getDownloadedModels(): Flow<List<ModelInfo>> = MutableStateFlow(emptyList())
    override suspend fun getSelectedModel(): ModelInfo? = selected
    override suspend fun selectModel(modelId: String) {}
    override fun getDownloadProgress(modelId: String): Flow<DownloadState> =
        flow { emit(DownloadState.Idle) }
    override fun loadCatalogFromJson(jsonContent: String) {}
}

// ---------------------------------------------------------------
// Tests
// ---------------------------------------------------------------

class GenerateInsightUseCaseTest {

    @Test
    fun `generate calls promptBuilder and inferenceRepository`() = runTest {
        val inferenceRepo = FakeInferenceRepository()
        val useCase = GenerateInsightUseCase(
            inferenceRepository = inferenceRepo,
            screenDataRepository = FakeScreenDataRepository(),
            modelRepository = FakeModelRepository(),
            promptBuilder = PromptBuilder()
        )

        val screenData = ScreenData(
            id = "s1",
            sourcePackage = "com.parkway.sg",
            sourceApp = "Parkway Pantai",
            textContent = "Bill SGD 150",
            timestamp = 1_700_000_000_000L
        )

        val result = useCase.generate(
            screenData = screenData,
            modelPath = "/models/test.gguf",
            chatTemplate = "qwen"
        ).first()

        assertEquals("insight text", result)
        assertEquals(1, inferenceRepo.generateCallCount)
        assertEquals("/models/test.gguf", inferenceRepo.lastModelPath)
        requireNotNull(inferenceRepo.lastPrompt)
        // prompt contains screen data
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "Bill SGD 150")
    }

    @Test
    fun `generateFromLatestScreen gets recent data and selected model`() = runTest {
        val inferenceRepo = FakeInferenceRepository()
        val screenDataRepo = FakeScreenDataRepository()
        val modelRepo = FakeModelRepository()
        val useCase = GenerateInsightUseCase(
            inferenceRepository = inferenceRepo,
            screenDataRepository = screenDataRepo,
            modelRepository = modelRepo,
            promptBuilder = PromptBuilder()
        )

        val result = useCase.generateFromLatestScreen().first()

        assertEquals("insight text", result)
        assertEquals(1, inferenceRepo.generateCallCount)
        // The selected model's downloaded path is used
        assertEquals("/data/models/finance-llama.gguf", inferenceRepo.lastModelPath)
        requireNotNull(inferenceRepo.lastPrompt)
        // screen data repo's recent data is included
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "OCBC Digital")
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "Account balance SGD 12,000.00")
    }

    @Test
    fun `generateFromLatestScreen throws when no model selected`() = runTest {
        val useCase = GenerateInsightUseCase(
            inferenceRepository = FakeInferenceRepository(),
            screenDataRepository = FakeScreenDataRepository(),
            modelRepository = FakeModelRepository(selected = null),
            promptBuilder = PromptBuilder()
        )

        val exception = assertFailsWith<IllegalStateException> {
            useCase.generateFromLatestScreen().first()
        }
        assertEquals("No model selected", exception.message)
    }

    @Test
    fun `generateFromLatestScreen throws when no screen data available`() = runTest {
        val useCase = GenerateInsightUseCase(
            inferenceRepository = FakeInferenceRepository(),
            screenDataRepository = FakeScreenDataRepository(data = emptyList()),
            modelRepository = FakeModelRepository(),
            promptBuilder = PromptBuilder()
        )

        val exception = assertFailsWith<IllegalStateException> {
            useCase.generateFromLatestScreen().first()
        }
        assertEquals("No screen data available", exception.message)
    }

    @Test
    fun `generateFromLatestScreen passes lora instruction to generate`() = runTest {
        val inferenceRepo = FakeInferenceRepository()
        val useCase = GenerateInsightUseCase(
            inferenceRepository = inferenceRepo,
            screenDataRepository = FakeScreenDataRepository(),
            modelRepository = FakeModelRepository(),
            promptBuilder = PromptBuilder()
        )

        useCase.generateFromLatestScreen(loraInstruction = "Focus on savings").first()

        requireNotNull(inferenceRepo.lastPrompt)
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "Additional instruction: Focus on savings")
    }

    @Test
    fun `getInsights delegates to repository`() = runTest {
        val insight = FinanceInsight(
            id = "ins-1",
            title = "High Spending Alert",
            summary = "You spent 30% more this month",
            detailText = "Detail text here",
            category = InsightCategory.SPENDING,
            timestamp = 1_700_000_000_000L
        )
        val insightsFlow = flow { emit(listOf(insight)) }
        val inferenceRepo = FakeInferenceRepository(insightsResult = insightsFlow)
        val useCase = GenerateInsightUseCase(
            inferenceRepository = inferenceRepo,
            screenDataRepository = FakeScreenDataRepository(),
            modelRepository = FakeModelRepository(),
            promptBuilder = PromptBuilder()
        )

        val insights = useCase.getInsights().first()
        assertEquals(1, insights.size)
        assertEquals("High Spending Alert", insights.first().title)
        assertEquals(InsightCategory.SPENDING, insights.first().category)
    }

    @Test
    fun `generate passes screenData and modelPath correctly`() = runTest {
        val inferenceRepo = FakeInferenceRepository()
        val useCase = GenerateInsightUseCase(
            inferenceRepository = inferenceRepo,
            screenDataRepository = FakeScreenDataRepository(),
            modelRepository = FakeModelRepository(),
            promptBuilder = PromptBuilder()
        )

        val screenData = ScreenData(
            id = "s2",
            sourcePackage = "com.grab",
            sourceApp = "Grab",
            textContent = "Ride SGD 12.50",
            timestamp = 1_700_000_000_001L
        )

        useCase.generate(screenData, "/custom/path.gguf", "mistral").first()

        assertEquals("/custom/path.gguf", inferenceRepo.lastModelPath)
        requireNotNull(inferenceRepo.lastPrompt)
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "Grab")
        kotlin.test.assertContains(inferenceRepo.lastPrompt, "[INST]")
    }
}