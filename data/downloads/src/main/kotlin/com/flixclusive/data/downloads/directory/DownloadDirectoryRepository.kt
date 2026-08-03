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

    /** Resolves a persisted SAF document URI (e.g. [com.flixclusive.core.database.entity.downloads.DownloadItem.streamFilePath])
     * back to a [UniFile], without creating anything on miss — unlike [getOrCreateFile]. */
    fun resolveFile(uri: String): UniFile?

    /** Lists the subtitle files sitting alongside [streamFile], by walking to its parent
     * directory and listing its `subtitles/` folder. */
    fun listSubtitleFiles(streamFile: UniFile): List<UniFile>
}
