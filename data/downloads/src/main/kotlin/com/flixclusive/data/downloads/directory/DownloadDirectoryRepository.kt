package com.flixclusive.data.downloads.directory

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.hippo.unifile.UniFile

interface DownloadDirectoryRepository {
    fun getOrCreateMediaDirectory(
        root: UniFile,
        media: MediaMetadata,
        episode: Episode? = null,
    ): UniFile?

    fun getOrCreateSubtitlesDirectory(mediaDirectory: UniFile): UniFile?
}
