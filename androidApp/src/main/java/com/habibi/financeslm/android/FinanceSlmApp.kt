package com.habibi.financeslm.android

import android.app.Application
import com.habibi.financeslm.android.di.androidAppModule
import com.habibi.financeslm.di.appModule
import com.habibi.financeslm.di.sdkModule
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.platform.PlatformContext
import com.habibi.financeslm.util.Logger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

class FinanceSlmApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PlatformContext
        PlatformContext.init(this)

        // Initialize Koin DI
        startKoin {
            androidContext(this@FinanceSlmApp)
            modules(sdkModule, appModule, androidAppModule)
        }

        // Load the model catalog from bundled assets into the repository
        loadModelCatalog()
    }

    /**
     * Read the model_catalog.json from assets and load it into the ModelRepository.
     */
    private fun loadModelCatalog() {
        try {
            val jsonContent = assets.open("model_catalog.json").bufferedReader().use { it.readText() }
            val repo = get<ModelRepository>(ModelRepository::class.java)
            repo.loadCatalogFromJson(jsonContent)
            Logger.d("FinanceSlmApp", "Model catalog loaded from assets")
        } catch (e: Exception) {
            Logger.e("FinanceSlmApp", "Failed to load model catalog", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            try {
                val repo = get<InferenceRepository>(InferenceRepository::class.java)
                kotlinx.coroutines.runBlocking {
                    repo.unloadModel()
                }
                Logger.d("FinanceSlmApp", "Model unloaded on trim memory (level=$level)")
            } catch (e: Exception) {
                Logger.e("FinanceSlmApp", "Error unloading model on trim memory", e)
            }
        }
    }
}