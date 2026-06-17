package com.habibi.financeslm.data.repository

import com.habibi.financeslm.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific download enqueuer interface.
 * On Android, the actual implementation uses WorkManager (provided via DI from androidApp module).
 * On iOS, a direct coroutine-based implementation is used.
 */
interface DownloadEnqueuer {
    fun enqueueDownload(
        url: String,
        destinationPath: String,
        expectedSha256: String,
        modelId: String
    ): Flow<DownloadState>

    suspend fun cancelDownload(modelId: String)
}
