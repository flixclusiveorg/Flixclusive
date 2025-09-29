package com.flixclusive.data.downloads.model

import com.flixclusive.core.common.locale.UiText
import java.io.File

data class DownloadState(
    val downloadId: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: UiText? = null,
    val file: File? = null,
) {
    companion object {
        val IDLE = DownloadState(downloadId = "")

        fun DownloadState.error(error: UiText) =
            copy(
                status = DownloadStatus.FAILED,
                error = error,
            )
    }
}
