package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.util.ChecksumVerifier
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Stub iOS ModelDownloadDataSource — will use Ktor HttpClient on iOS.
 *
 * Real implementation requires Ktor with Darwin engine for URL loading.
 * For now, returns a stub Done state so wiring compiles.
 */
actual class ModelDownloadDataSource actual constructor(
    private val checksumVerifier: ChecksumVerifier,
    private val fileSystem: FileSystem
) {
    actual fun download(url: String, destinationPath: String, expectedSha256: String): Flow<DownloadState> = callbackFlow {
        Logger.d("DownloadDS(iOS)", "Stub download: $url -> $destinationPath")
        trySend(DownloadState.Done)
        close()
    }

    actual suspend fun cancel(modelId: String) {
        Logger.d("DownloadDS(iOS)", "Stub cancel: $modelId")
    }
}