package com.flixclusive.data.downloads.repository

import com.flixclusive.data.downloads.model.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Repository responsible for handling file downloads, including starting,
 * monitoring progress, and cancelling downloads.
 *
 * @see DownloadState
 * */
interface DownloadRepository {
    /**
     * Returns a [StateFlow] that emits the current [DownloadState] for the given [id].
     *
     * @param id A unique identifier for the download task.
     *
     * @return A [StateFlow] emitting the current [DownloadState].
     * */
    fun getDownloadState(id: String): Flow<DownloadState>

    /**
     * Initiates a file download from the specified [url] and saves it to [destinationFile].
     * The download progress and state can be monitored via [getDownloadState].
     *
     * @param id A unique identifier for the download task.
     * @param url The URL from which to download the file.
     * @param destinationFile The file where the downloaded content will be saved.
     *
     * @throws Exception if the download fails for any reason.
     * */
    fun executeDownload(
        id: String,
        url: String,
        destinationFile: File,
    )

    /**
     * Cancels an ongoing download identified by [id].
     * If the download is in progress, it will be stopped and the state will be updated accordingly.
     *
     * @param id A unique identifier for the download task to be cancelled.
     * */
    fun cancelDownload(id: String)
}
