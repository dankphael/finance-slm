package com.habibi.financeslm.di

import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.LoraRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.PreferencesRepository
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.domain.usecase.CheckPermissionsUseCase
import com.habibi.financeslm.domain.usecase.DeleteModelUseCase
import com.habibi.financeslm.domain.usecase.DownloadModelUseCase
import com.habibi.financeslm.domain.usecase.GenerateInsightUseCase
import com.habibi.financeslm.domain.usecase.ManageLoraUseCase
import com.habibi.financeslm.domain.usecase.OnboardingStateUseCase
import com.habibi.financeslm.inference.createLlamaEngine
import com.habibi.financeslm.platform.createDeviceInfo
import com.habibi.financeslm.platform.createFileSystem
import com.habibi.financeslm.platform.createPlatformContext
import com.habibi.financeslm.platform.createScreenReader
import com.habibi.financeslm.prompt.PromptBuilder
import com.habibi.financeslm.util.createChecksumVerifier
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared common module — installed in all platforms.
 * PlatformModule (provided per-platform) is separate.
 */
val appModule = module {
    // Prompt
    single { PromptBuilder() }

    // Use cases
    factory { DownloadModelUseCase(get()) }
    factory { DeleteModelUseCase(get()) }
    factory { ManageLoraUseCase(get()) }
    factory { CheckPermissionsUseCase(get()) }
    factory { OnboardingStateUseCase(get(), get()) }
    factory { GenerateInsightUseCase(get(), get(), get(), get()) }

    // Utilities
    single { createChecksumVerifier() }
}

/**
 * Submodule for shared SDK/service instances.
 */
val sdkModule = module {
    single { createPlatformContext() }
    single { createFileSystem(get()) }
    single { createDeviceInfo() }
    single { createScreenReader() }
    single { createLlamaEngine() }
}

fun getSharedModules(): List<Module> = listOf(sdkModule, appModule)