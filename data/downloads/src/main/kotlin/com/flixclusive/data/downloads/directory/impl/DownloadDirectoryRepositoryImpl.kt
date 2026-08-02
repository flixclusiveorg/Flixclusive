package com.flixclusive.data.downloads.directory.impl

import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.hippo.unifile.UniFile
import javax.inject.Inject

internal class DownloadDirectoryRepositoryImpl @Inject constructor() : DownloadDirectoryRepository {
    override fun getOrCreateMediaDirectory(
        root: UniFile,
        media: MediaMetadata,
        episode: Episode?,
    ): UniFile? {
        val downloadsDir = root.getOrCreateChildDirectory(DOWNLOADS_FOLDER_NAME) ?: return null
        val mediaDir =
            downloadsDir.getOrCreateChildDirectory(DownloadPathUtil.buildMediaFolderName(media)) ?: return null

        if (episode == null) return mediaDir

        return mediaDir.getOrCreateChildDirectory(DownloadPathUtil.buildEpisodeFolderName(episode))
    }

    override fun getOrCreateSubtitlesDirectory(mediaDirectory: UniFile): UniFile? =
        mediaDirectory.getOrCreateChildDirectory(SUBTITLES_FOLDER_NAME)

    private fun UniFile.getOrCreateChildDirectory(name: String): UniFile? = findFile(name) ?: createDirectory(name)

    companion object {
        private const val DOWNLOADS_FOLDER_NAME = "Downloads"
        private const val SUBTITLES_FOLDER_NAME = "subtitles"
    }
}
