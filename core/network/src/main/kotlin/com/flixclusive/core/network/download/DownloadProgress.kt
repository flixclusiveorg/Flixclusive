package com.flixclusive.core.network.download

/**
 * Data class representing the progress of a download operation.
 *
 * @param bytesDownloaded The number of bytes that have been downloaded so far.
 * @param totalBytes The total number of bytes to be downloaded.
 * @param progress The download progress as a percentage (0-100).
 * @param isComplete A boolean indicating whether the download is complete.
 * */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Int,
    val isComplete: Boolean,
)
