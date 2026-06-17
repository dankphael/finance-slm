package com.habibi.financeslm.android.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.habibi.financeslm.data.datasource.ModelDownloadDataSource
import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.platform.PlatformContext
import com.habibi.financeslm.util.ChecksumVerifier
import java.io.File

/**
 * ModelDownloadWorker — downloads a model file using WorkManager's foreground
 * service integration, ensuring downloads survive app backgrounding.
 *
 * Input data keys:
 *  - "url"             : String — model file URL
 *  - "destination"     : String — destination file path
 *  - "expected_sha256" : String — SHA256 checksum (empty string = skip verification)
 *  - "model_id"        : String — model ID for tracking
 *
 * Progress data keys:
 *  - "progress"  : Float — 0.0 to 1.0
 *  - "bytes"     : Long  — bytes downloaded
 *  - "total"     : Long  — total bytes (-1 if unknown)
 *  - "status"    : String — "downloading", "done", or "error"
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ModelDownloadWorker"

        // Input data keys
        const val KEY_URL = "url"
        const val KEY_DESTINATION = "destination"
        const val KEY_SHA256 = "expected_sha256"
        const val KEY_MODEL_ID = "model_id"

        // Progress data keys
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_STATUS = "status"

        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_DONE = "done"
        const val STATUS_ERROR = "error"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val destination = inputData.getString(KEY_DESTINATION) ?: return Result.failure()
        val expectedSha256 = inputData.getString(KEY_SHA256) ?: ""
        val modelId = inputData.getString(KEY_MODEL_ID) ?: "unknown"

        Log.d(TAG, "Worker starting download: model=$modelId url=$url")

        // Build dependencies using Koin-initialized singletons
        // PlatformContext is initialized by FinanceSlmApp.onCreate()
        val platformContext = PlatformContext.getInstance()
        val checksumVerifier = ChecksumVerifier()
        val fileSystem = FileSystem().apply { init(platformContext) }
        val downloadDataSource = ModelDownloadDataSource(checksumVerifier, fileSystem)

        // Set foreground notification
        setForeground(createForegroundInfo("Downloading model..."))

        try {
            downloadDataSource.download(url, destination, expectedSha256).collect { state ->
                when (state) {
                    is DownloadState.Downloading -> {
                        val progress = state.progress.coerceIn(0f, 1f)
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to progress,
                                KEY_BYTES to state.bytesDownloaded,
                                KEY_TOTAL to state.totalBytes,
                                KEY_STATUS to STATUS_DOWNLOADING
                            )
                        )
                    }
                    is DownloadState.Idle -> {
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to 0f,
                                KEY_STATUS to STATUS_DOWNLOADING
                            )
                        )
                    }
                    is DownloadState.Done -> {
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to 1f,
                                KEY_STATUS to STATUS_DONE
                            )
                        )
                        Log.d(TAG, "Download complete: $modelId")
                    }
                    is DownloadState.Error -> {
                        Log.e(TAG, "Download error: ${state.message}")
                        setProgress(
                            workDataOf(
                                KEY_STATUS to STATUS_ERROR,
                                KEY_PROGRESS to 0f
                            )
                        )
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker exception: ${e.message}", e)
            return Result.failure()
        }
    }

    /**
     * Create foreground service notification for the download.
     */
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "finance_slm_model_download"
        )
            .setContentTitle("Downloading Model")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(false)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(1002, notification)
    }
}