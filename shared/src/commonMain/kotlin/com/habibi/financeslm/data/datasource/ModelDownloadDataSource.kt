package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.util.ChecksumVerifier
import kotlinx.coroutines.flow.Flow

/**
 * ModelDownloadDataSource — downloads model files from HuggingFace (or any URL).
 *
 * Platform-specific: uses java.net.HttpURLConnection on Android,
 * Ktor HttpClient on iOS.
 */
expect class ModelDownloadDataSource(
    checksumVerifier: ChecksumVerifier,
    fileSystem: FileSystem
) {
    fun download(url: String, destinationPath: String, expectedSha256: String): Flow<DownloadState>

    suspend fun cancel(modelId: String)
}