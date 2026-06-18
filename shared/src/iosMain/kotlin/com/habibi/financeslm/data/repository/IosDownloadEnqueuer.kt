package com.habibi.financeslm.data.repository

import com.habibi.financeslm.data.datasource.httpDownload
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.util.ChecksumVerifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.collect

/**
 * iOS [DownloadEnqueuer] — the iOS counterpart to Android's
 * WorkManagerDownloadEnqueuer. iOS has no WorkManager, so downloads run as
 * coroutine flows backed by Ktor (Darwin) via [httpDownload]. Each in-flight
 * download's [Job] is tracked by modelId so [cancelDownload] can stop it.
 *
 * (iOS background-download continuation via URLSession is a future enhancement;
 * for now a download lives for as long as its collector is active.)
 */
class IosDownloadEnqueuer(
    private val checksumVerifier: ChecksumVerifier
) : DownloadEnqueuer {

    private val activeJobs = mutableMapOf<String, Job>()

    override fun enqueueDownload(
        url: String,
        destinationPath: String,
        expectedSha256: String,
        modelId: String
    ): Flow<DownloadState> = channelFlow {
        activeJobs[modelId] = coroutineContext.job
        try {
            httpDownload(url, destinationPath, expectedSha256, checksumVerifier).collect { state ->
                send(state)
            }
        } finally {
            activeJobs.remove(modelId)
        }
    }

    override suspend fun cancelDownload(modelId: String) {
        activeJobs.remove(modelId)?.cancel()
    }
}
