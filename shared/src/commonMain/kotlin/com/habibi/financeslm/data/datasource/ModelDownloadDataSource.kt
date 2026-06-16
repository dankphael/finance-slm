package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub ModelDownloadDataSource for compilation.
 * Real implementation will use Ktor with Range headers for resumable downloads.
 */
class ModelDownloadDataSource {
    fun download(url: String, destinationPath: String, expectedSha256: String): Flow<DownloadState> {
        Logger.d("DownloadDS", "download(stub): $url -> $destinationPath")
        return MutableStateFlow(DownloadState.Done).asStateFlow()
    }

    suspend fun cancel(modelId: String) {
        Logger.d("DownloadDS", "cancel(stub): $modelId")
    }
}