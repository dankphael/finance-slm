package com.habibi.financeslm.android.di

import com.habibi.financeslm.data.repository.InferenceRepositoryImpl
import com.habibi.financeslm.data.repository.LoraRepositoryImpl
import com.habibi.financeslm.data.repository.ModelRepositoryImpl
import com.habibi.financeslm.data.repository.PreferencesRepositoryImpl
import com.habibi.financeslm.data.repository.ScreenDataRepositoryImpl
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.LoraRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.PreferencesRepository
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.platform.createScreenReader
import org.koin.dsl.module

/**
 * Android-specific DI bindings for repository implementations and platform services.
 */
val androidAppModule = module {
    // Repository implementations
    single<ModelRepository> { ModelRepositoryImpl() }
    single<LoraRepository> { LoraRepositoryImpl() }
    single<InferenceRepository> { InferenceRepositoryImpl() }
    single<ScreenDataRepository> { ScreenDataRepositoryImpl() }
    single<PreferencesRepository> { PreferencesRepositoryImpl() }
}