package com.flixclusive.domain.downloads.model

/**
 * Data class representing a request to download a file.
 *
 * @property downloadId A unique identifier for the download request.
 * @property url The URL from which to download the file.
 * @property fileName The name to save the downloaded file as.
 * @property destinationPath The path where the file should be saved.
 * */
data class DownloadRequest(
    val downloadId: String,
    val url: String,
    val fileName: String,
    val destinationPath: String,
) {
    companion object {
        /**
         * Factory method to create a DownloadRequest instance.
         *
         * @param url The URL from which to download the file.
         * @param fileName The name to save the downloaded file as.
         * @param destinationPath The path where the file should be saved.
         *
         * @return A new instance of [DownloadRequest] with a unique [downloadId].
         * */
        fun from(
            url: String,
            fileName: String,
            destinationPath: String,
        ) = DownloadRequest(
            downloadId = getDownloadId(url, fileName, destinationPath),
            url = url,
            fileName = fileName,
            destinationPath = destinationPath,
        )

        /**
         * Generates a unique download ID based on the URL, file name, and destination path.
         *
         * @param url The URL from which to download the file.
         * @param fileName The name to save the downloaded file as.
         * @param destinationPath The path where the file should be saved.
         *
         * @return A unique string identifier for the download request.
         * */
        fun getDownloadId(
            url: String,
            fileName: String,
            destinationPath: String,
        ): String = "$url|$fileName|$destinationPath".hashCode().toString()
    }
}
