package com.habibi.financeslm.di

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.domain.model.FinanceInsight
import com.habibi.financeslm.domain.model.ModelInfo
import com.habibi.financeslm.domain.model.ScreenData
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.PreferencesRepository
import com.habibi.financeslm.domain.usecase.DownloadModelUseCase
import com.habibi.financeslm.domain.usecase.GenerateInsightUseCase
import com.habibi.financeslm.domain.usecase.ManageLoraUseCase
import com.habibi.financeslm.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * iOS Koin entry point — call once at app launch from Swift:
 *   KoinIosKt.doInitKoinIos()
 */
fun initKoinIos() {
    startKoin {
        modules(sdkModule, appModule, iosAppModule)
    }
}

/**
 * Swift-facing facade over the shared SDK. Resolving Koin `get()` from Swift is
 * awkward, so this exposes the use cases the SwiftUI app needs plus a few typed
 * convenience methods. Kotlin `suspend` functions bridge to Swift `async`, and
 * the typed return values ([ModelInfo] etc.) are far nicer to consume from Swift
 * than raw Flows. Construct it from Swift *after* [initKoinIos]:  let sdk = SharedSdk()
 */
class SharedSdk : KoinComponent {
    val modelRepository: ModelRepository by inject()
    val inferenceRepository: InferenceRepository by inject()
    val preferencesRepository: PreferencesRepository by inject()
    val generateInsightUseCase: GenerateInsightUseCase by inject()
    val downloadModelUseCase: DownloadModelUseCase by inject()
    val manageLoraUseCase: ManageLoraUseCase by inject()

    /** Load the bundled model catalog JSON (read from the iOS app bundle). */
    fun loadCatalog(json: String) {
        modelRepository.loadCatalogFromJson(json)
    }

    // ── Typed snapshots (suspend -> Swift async) ──────────────────────────────

    suspend fun catalogSnapshot(): List<ModelInfo> = modelRepository.getCatalog().first()

    suspend fun downloadedSnapshot(): List<ModelInfo> = modelRepository.getDownloadedModels().first()

    suspend fun selectedModel(): ModelInfo? = modelRepository.getSelectedModel()

    suspend fun selectModel(modelId: String) = modelRepository.selectModel(modelId)

    // ── Flows (wrap with SwiftFlow on the Swift side) ─────────────────────────

    suspend fun downloadModel(modelId: String): Flow<DownloadState> =
        modelRepository.downloadModel(modelId)

    /**
     * Generate an insight from free-typed text (iOS has no screen-reading, so
     * the app supplies the context manually). Returns a token stream.
     */
    suspend fun generateInsight(text: String, modelPath: String, chatTemplate: String): Flow<String> {
        val screenData = ScreenData(
            id = "ios_${currentTimeMillis()}",
            sourcePackage = "manual.input",
            sourceApp = "Manual Input",
            textContent = text,
            timestamp = currentTimeMillis()
        )
        return generateInsightUseCase.generate(screenData, modelPath, chatTemplate)
    }

    fun insights(): Flow<List<FinanceInsight>> = inferenceRepository.getInsights()
}
