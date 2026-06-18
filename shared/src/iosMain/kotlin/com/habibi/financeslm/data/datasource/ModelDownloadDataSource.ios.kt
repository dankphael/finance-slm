package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.util.ChecksumVerifier
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * iOS ModelDownloadDataSource — real Ktor (Darwin) streaming download via
 * [httpDownload]. The primary download path goes through IosDownloadEnqueuer
 * (which ModelRepositoryImpl calls); this data source is used by
 * getDownloadProgress, which passes an empty URL to probe local state.
 */
actual class ModelDownloadDataSource actual constructor(
    private val checksumVerifier: ChecksumVerifier,
    private val fileSystem: FileSystem
) {
    actual fun download(url: String, destinationPath: String, expectedSha256: String): Flow<DownloadState> {
        if (url.isEmpty()) {
            // getDownloadProgress() probes with an empty URL — just report local state.
            return flow {
                emit(if (fileSystem.fileExists(destinationPath)) DownloadState.Done else DownloadState.Idle)
            }
        }
        return httpDownload(url, destinationPath, expectedSha256, checksumVerifier)
    }

    actual suspend fun cancel(modelId: String) {
        // Cancellation is driven by cancelling the collecting coroutine (see
        // IosDownloadEnqueuer.cancelDownload). Nothing to release here.
        Logger.d("DownloadDS(iOS)", "cancel: $modelId")
    }
}
