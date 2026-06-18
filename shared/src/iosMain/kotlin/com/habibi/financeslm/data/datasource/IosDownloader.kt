package com.habibi.financeslm.data.datasource

import com.habibi.financeslm.domain.model.DownloadState
import com.habibi.financeslm.util.ChecksumVerifier
import com.habibi.financeslm.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import okio.FileSystem as OkioFileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Shared iOS HTTP downloader used by both [ModelDownloadDataSource] and
 * IosDownloadEnqueuer. Streams the response straight to disk via okio (so large
 * .gguf files never sit in memory), reports progress, and verifies SHA-256.
 *
 * NOTE: builds against Ktor 3 (Darwin engine) and okio. The streaming-read API
 * (`ByteReadChannel.readAvailable`) and a couple of Foundation/okio calls can
 * only be compiled on a Mac — verify there.
 */
internal val iosHttpClient: HttpClient by lazy {
    HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }
}

internal fun httpDownload(
    url: String,
    destinationPath: String,
    expectedSha256: String,
    checksumVerifier: ChecksumVerifier
): Flow<DownloadState> = channelFlow {
    val fs = OkioFileSystem.SYSTEM
    val dest = destinationPath.toPath()
    try {
        dest.parent?.let { fs.createDirectories(it) }

        // Already fully downloaded? Verify and short-circuit.
        if (fs.exists(dest) && (fs.metadataOrNull(dest)?.size ?: 0L) > 0L) {
            if (expectedSha256.isEmpty() || checksumVerifier.verify(destinationPath, expectedSha256)) {
                send(DownloadState.Done)
                return@channelFlow
            }
            fs.delete(dest)
        }

        Logger.d("IosDownloader", "GET $url -> $destinationPath")
        iosHttpClient.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value}")
            }
            val total = response.contentLength() ?: -1L
            val channel = response.bodyAsChannel()
            var downloaded = 0L
            var sinceEmit = 0L

            send(DownloadState.Downloading(progress = 0f, bytesDownloaded = 0L, totalBytes = total))

            fs.sink(dest).buffer().use { sink ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val read = channel.readAvailable(buf, 0, buf.size)
                    if (read == -1) break
                    if (read > 0) {
                        sink.write(buf, 0, read)
                        downloaded += read
                        sinceEmit += read
                        if (sinceEmit >= 256 * 1024) {
                            val progress = if (total > 0L) downloaded.toFloat() / total else -1f
                            send(DownloadState.Downloading(progress, downloaded, total))
                            sinceEmit = 0L
                        }
                    }
                }
                sink.flush()
            }
            if (total > 0L) {
                send(DownloadState.Downloading(1f, total, total))
            }
        }

        if (expectedSha256.isNotEmpty() && !checksumVerifier.verify(destinationPath, expectedSha256)) {
            fs.delete(dest)
            send(DownloadState.Error("SHA256 checksum mismatch"))
            return@channelFlow
        }

        send(DownloadState.Done)
    } catch (e: CancellationException) {
        Logger.d("IosDownloader", "Download cancelled: $destinationPath")
        throw e
    } catch (e: Exception) {
        Logger.e("IosDownloader", "Download failed: ${e.message}")
        send(DownloadState.Error(e.message ?: "Download failed"))
    }
}
