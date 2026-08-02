package com.flixclusive.data.downloads.directory

import com.hippo.unifile.UniFile

interface DownloadDirectoryRepository {
    fun getOrCreateMediaDirectory(
        root: UniFile,
        mediaId: String,
        mediaTitle: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
    ): UniFile?

    fun getOrCreateSubtitlesDirectory(mediaDirectory: UniFile): UniFile?

    fun getOrCreateFile(
        directory: UniFile,
        fileName: String,
    ): UniFile?
}
