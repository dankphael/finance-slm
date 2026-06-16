package com.habibi.financeslm.android.di

import com.habibi.financeslm.android.ui.viewmodel.HomeViewModel
import com.habibi.financeslm.android.ui.viewmodel.LoraEditorViewModel
import com.habibi.financeslm.android.ui.viewmodel.ModelManagementViewModel
import com.habibi.financeslm.android.ui.viewmodel.OnboardingViewModel
import com.habibi.financeslm.data.datasource.ModelDownloadDataSource
import com.habibi.financeslm.data.datasource.ModelStorageDataSource
import com.habibi.financeslm.data.datasource.PreferencesDataSource
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
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.platform.ScreenReader
import com.habibi.financeslm.util.ChecksumVerifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specific DI bindings for repository implementations, platform services, and ViewModels.
 */
val androidAppModule = module {
    // ── Data Sources ──
    single { PreferencesDataSource() }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    single { ModelStorageDataSource(get()) }
    single { ModelDownloadDataSource(get<ChecksumVerifier>(), get<FileSystem>()) }

    // ── Repository implementations ──
    single<ModelRepository> {
        ModelRepositoryImpl(
            downloadDataSource = get(),
            storageDataSource = get(),
            preferencesDataSource = get(),
            fileSystem = get()
        )
    }
    single<LoraRepository> { LoraRepositoryImpl(get()) }
    single<InferenceRepository> {
        InferenceRepositoryImpl(
            llamaEngine = get(),
            promptBuilder = get()
        )
    }
    single<ScreenDataRepository> {
        ScreenDataRepositoryImpl(screenDataFlow = get<ScreenReader>().observeScreenData())
    }

    // ── ViewModels ──
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { LoraEditorViewModel(get()) }
    viewModel { ModelManagementViewModel(get(), get()) }
}