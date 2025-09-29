package com.flixclusive.data.downloads.model

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    ;

    val isFinished: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
    val isIdle get() = this == IDLE
    val isDownloading get() = this == DOWNLOADING
}
