package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.platform.FileSystem
import com.habibi.financeslm.util.ChecksumVerifier
import com.habibi.financeslm.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real ModelDownloadDataSource — downloads model files from HuggingFace (or any URL)
 * using java.net.HttpURLConnection with Range-header resume support,
 * progress tracking, and SHA256 verification.
 *
 * NOTE: This is the Android-specific implementation. The common expect declaration
 * lives in shared/src/commonMain.
 */
actual class ModelDownloadDataSource actual constructor(
    private val checksumVerifier: ChecksumVerifier,
    private val fileSystem: FileSystem
) {
    private val activeDownloads = mutableMapOf<String, Job>()

    /**
     * Download a model file from [url] to [destinationPath].
     *
     * Flow emits:
     * - [DownloadState.Downloading] with progress info during download
     * - [DownloadState.Done] on successful completion (with optional SHA256 verification)
     * - [DownloadState.Error] on failure
     *
     * Supports resume via HTTP Range header if a partial file already exists.
     */
    actual fun download(url: String, destinationPath: String, expectedSha256: String): Flow<DownloadState> = callbackFlow {
        val destFile = File(destinationPath)
        Logger.d("DownloadDS", "Starting download: $url -> $destinationPath")

        // 1. Check if already fully downloaded
        if (destFile.exists() && destFile.length() > 0L) {
            if (expectedSha256.isNotEmpty()) {
                Logger.d("DownloadDS", "File exists, verifying SHA256...")
                val valid = checksumVerifier.verify(destinationPath, expectedSha256)
                if (valid) {
                    Logger.d("DownloadDS", "File exists and SHA256 matches")
                    trySend(DownloadState.Done)
                    close()
                    return@callbackFlow
                } else {
                    Logger.d("DownloadDS", "SHA256 mismatch, re-downloading")
                    destFile.delete()
                }
            } else {
                Logger.d("DownloadDS", "File exists, no SHA256 to verify — assuming complete")
                trySend(DownloadState.Done)
                close()
                return@callbackFlow
            }
        }

        // 2. Ensure parent directory exists
        destFile.parentFile?.mkdirs()

        // 3. Check for partial file (resume support)
        val existingBytes = if (destFile.exists()) destFile.length() else 0L

        try {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000

                    if (existingBytes > 0L) {
                        connection.setRequestProperty("Range", "bytes=$existingBytes-")
                        Logger.d("DownloadDS", "Resuming from byte $existingBytes")
                    }

                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..206) {
                        trySend(DownloadState.Error("HTTP $responseCode downloading $url"))
                        close()
                        return@withContext
                    }

                    val contentLength = connection.contentLengthLong
                    val fullSize: Long = if (existingBytes > 0L && contentLength > 0L) {
                        existingBytes + contentLength
                    } else if (contentLength > 0L) {
                        contentLength
                    } else {
                        -1L
                    }

                    Logger.d("DownloadDS", "Total size: $fullSize, Resumed: $existingBytes")
                    trySend(DownloadState.Downloading(
                        progress = if (existingBytes > 0L && fullSize > 0L) existingBytes.toFloat() / fullSize else 0f,
                        bytesDownloaded = existingBytes,
                        totalBytes = fullSize
                    ))

                    val inputStream = BufferedInputStream(connection.inputStream)
                    val outputStream = BufferedOutputStream(FileOutputStream(destFile, existingBytes > 0L))

                    try {
                        var bytesSinceLastEmit = 0L
                        var bytesDownloaded = existingBytes
                        val buffer = ByteArray(32 * 1024) // 32KB buffer

                        while (isActive) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            outputStream.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead
                            bytesSinceLastEmit += bytesRead

                            // Emit progress every ~256KB to avoid flooding
                            if (bytesSinceLastEmit >= 256 * 1024) {
                                val progress = if (fullSize > 0L) bytesDownloaded.toFloat() / fullSize else -1f
                                trySend(DownloadState.Downloading(
                                    progress = progress,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = fullSize
                                ))
                                bytesSinceLastEmit = 0L
                            }
                        }

                        outputStream.flush()

                        // Final progress
                        if (fullSize > 0L) {
                            trySend(DownloadState.Downloading(
                                progress = 1f,
                                bytesDownloaded = fullSize,
                                totalBytes = fullSize
                            ))
                        }

                        Logger.d("DownloadDS", "Download completed: $destinationPath ($bytesDownloaded bytes)")

                        // SHA256 verification
                        if (expectedSha256.isNotEmpty()) {
                            Logger.d("DownloadDS", "Verifying SHA256...")
                            val valid = checksumVerifier.verify(destinationPath, expectedSha256)
                            if (!valid) {
                                Logger.e("DownloadDS", "SHA256 mismatch for $destinationPath")
                                destFile.delete()
                                trySend(DownloadState.Error("SHA256 checksum mismatch"))
                                close()
                                return@withContext
                            }
                            Logger.d("DownloadDS", "SHA256 verified OK")
                        }

                        trySend(DownloadState.Done)
                    } catch (e: CancellationException) {
                        Logger.d("DownloadDS", "Download cancelled: $destinationPath")
                        trySend(DownloadState.Error("Download cancelled", e))
                        throw e
                    } catch (e: Exception) {
                        Logger.e("DownloadDS", "Download failed: $destinationPath", e)
                        trySend(DownloadState.Error(e.message ?: "Download failed", e))
                    } finally {
                        try { inputStream.close() } catch (_: Exception) {}
                        try { outputStream.close() } catch (_: Exception) {}
                        close()
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: CancellationException) {
            Logger.d("DownloadDS", "Download cancelled (outer): $destinationPath")
            trySend(DownloadState.Error("Download cancelled", e))
            close()
        } catch (e: Exception) {
            Logger.e("DownloadDS", "Download error: $destinationPath", e)
            trySend(DownloadState.Error(e.message ?: "Download error", e))
            close()
        }
    }

    /**
     * Cancel an active download for [modelId] by cancelling its coroutine Job.
     */
    actual suspend fun cancel(modelId: String) {
        val job = activeDownloads.remove(modelId)
        if (job != null) {
            job.cancel()
            Logger.d("DownloadDS", "Cancelled download for model: $modelId")
        }
    }
}