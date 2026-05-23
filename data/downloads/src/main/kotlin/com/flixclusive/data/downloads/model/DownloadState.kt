package com.flixclusive.data.downloads.model

import com.flixclusive.core.common.locale.UiText
import java.io.File

data class DownloadState(
    val id: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: UiText? = null,
    val file: File? = null,
) {
    companion object {
        val IDLE = DownloadState(id = "")

        fun DownloadState.error(error: UiText) =
            copy(
                status = DownloadStatus.FAILED,
                error = error,
            )
    }
}
