package com.habibi.financeslm.di

import com.habibi.financeslm.data.datasource.ModelDownloadDataSource
import com.habibi.financeslm.data.datasource.ModelStorageDataSource
import com.habibi.financeslm.data.datasource.PreferencesDataSource
import com.habibi.financeslm.data.repository.DownloadEnqueuer
import com.habibi.financeslm.data.repository.InferenceRepositoryImpl
import com.habibi.financeslm.data.repository.IosDownloadEnqueuer
import com.habibi.financeslm.data.repository.LoraRepositoryImpl
import com.habibi.financeslm.data.repository.ModelRepositoryImpl
import com.habibi.financeslm.data.repository.PreferencesRepositoryImpl
import com.habibi.financeslm.data.repository.ScreenDataRepositoryImpl
import com.habibi.financeslm.db.DatabaseDriverFactory
import com.habibi.financeslm.domain.repository.InferenceRepository
import com.habibi.financeslm.domain.repository.LoraRepository
import com.habibi.financeslm.domain.repository.ModelRepository
import com.habibi.financeslm.domain.repository.PreferencesRepository
import com.habibi.financeslm.domain.repository.ScreenDataRepository
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.util.ChecksumVerifier
import org.koin.dsl.module

/**
 * iOS platform DI module — the iOS counterpart to androidAppModule. Wires the
 * data sources, repositories, download enqueuer, and database driver with
 * iOS-appropriate constructions (no androidx ViewModels; the SwiftUI app drives
 * the shared use cases directly).
 */
val iosAppModule = module {
    // ── Data sources ──
    single { PreferencesDataSource() }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    single { ModelStorageDataSource(get()) }
    single { ModelDownloadDataSource(get<ChecksumVerifier>(), get<FileSystem>()) }

    // ── Download enqueuer (iOS uses a coroutine/Ktor-backed downloader) ──
    single<DownloadEnqueuer> { IosDownloadEnqueuer(get()) }

    // ── Repositories ──
    single<ModelRepository> {
        ModelRepositoryImpl(
            downloadDataSource = get(),
            storageDataSource = get(),
            preferencesDataSource = get(),
            fileSystem = get(),
            downloadEnqueuer = get()
        )
    }
    single<LoraRepository> { LoraRepositoryImpl(get()) }
    single<InferenceRepository> {
        InferenceRepositoryImpl(
            llamaEngine = get(),
            database = get()
        )
    }
    single<ScreenDataRepository> { ScreenDataRepositoryImpl() }

    // ── Database ──
    single { DatabaseDriverFactory() }
}
