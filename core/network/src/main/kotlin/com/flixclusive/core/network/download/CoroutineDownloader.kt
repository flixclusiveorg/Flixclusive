package com.flixclusive.core.network.download

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface defining a coroutine-based file downloader.
 *
 * The `download` function initiates the download of a file from the specified URL
 * and saves it to the given destination file. It returns a Flow of [DownloadProgress]
 * objects that provide updates on the download progress.
 * */
interface CoroutineDownloader {
    /**
     * Downloads a file from the specified URL and saves it to the given destination file.
     *
     * @param url The URL of the file to download.
     * @param destinationFile The file where the downloaded content will be saved.
     *
     * @return A Flow of [DownloadProgress] objects that provide updates on the download progress.
     * */
    fun download(
        url: String,
        destinationFile: File,
    ): Flow<DownloadProgress>
}
