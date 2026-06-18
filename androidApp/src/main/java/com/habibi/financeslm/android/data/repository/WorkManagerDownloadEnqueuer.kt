package com.habibi.financeslm.android.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.habibi.financeslm.android.service.ModelDownloadWorker
import com.habibi.financeslm.data.repository.DownloadEnqueuer
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Android [DownloadEnqueuer] — uses WorkManager to download model files in a
 * foreground service, ensuring downloads survive app backgrounding.
 *
 * This implementation lives in the androidApp module to access androidx.work
 * classes which are not available in the shared module.
 */
class WorkManagerDownloadEnqueuer(
    private val context: Context
) : DownloadEnqueuer {
    private val workManager = WorkManager.getInstance(context)

    // Track active download work IDs by model ID
    private val activeWorkIds = mutableMapOf<String, UUID>()

    /**
     * Enqueue a model download via WorkManager and return a Flow of download states.
     * The worker runs as a foreground service so it survives app backgrounding.
     */
    override fun enqueueDownload(
        url: String,
        destinationPath: String,
        expectedSha256: String,
        modelId: String
    ): Flow<DownloadState> {
        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_URL, url)
            .putString(ModelDownloadWorker.KEY_DESTINATION, destinationPath)
            .putString(ModelDownloadWorker.KEY_SHA256, expectedSha256)
            .putString(ModelDownloadWorker.KEY_MODEL_ID, modelId)
            .build()

        val workRequest: WorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .addTag("model_download_$modelId")
            .build()

        // Store the work ID for cancellation tracking
        activeWorkIds[modelId] = workRequest.id

        Logger.d("DownloadEnqueuer", "Enqueuing download: model=$modelId, workId=${workRequest.id}")

        // Enqueue the work
        workManager.enqueue(workRequest)

        // Return a Flow that maps WorkManager's WorkInfo to DownloadState
        return workManager.getWorkInfoByIdFlow(workRequest.id).map { workInfo ->
            if (workInfo == null) {
                DownloadState.Error("WorkInfo not available")
            } else {
                val progress = workInfo.progress
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.RUNNING -> {
                        val p = progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                        val bytes = progress.getLong(ModelDownloadWorker.KEY_BYTES, 0L)
                        val total = progress.getLong(ModelDownloadWorker.KEY_TOTAL, -1L)
                        DownloadState.Downloading(progress = p, bytesDownloaded = bytes, totalBytes = total)
                    }
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        DownloadState.Done
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                        val errorMsg = progress.getString(ModelDownloadWorker.KEY_STATUS) ?: "Download failed"
                        DownloadState.Error(errorMsg)
                    }
                    androidx.work.WorkInfo.State.CANCELLED -> {
                        DownloadState.Error("Download cancelled")
                    }
                    else -> {
                        DownloadState.Downloading(progress = 0f, bytesDownloaded = 0L, totalBytes = -1L)
                    }
                }
            }
        }
    }

    /**
     * Cancel a download by model ID.
     */
    override suspend fun cancelDownload(modelId: String) {
        val workId = activeWorkIds.remove(modelId)
        if (workId != null) {
            workManager.cancelWorkById(workId)
            Logger.d("DownloadEnqueuer", "Cancelled download: model=$modelId")
        }
    }
}
