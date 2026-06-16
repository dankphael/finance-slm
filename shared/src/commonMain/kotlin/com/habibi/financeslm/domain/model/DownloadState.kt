package com.habibi.financeslm.domain.model

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data object Done : DownloadState()
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadState()
}