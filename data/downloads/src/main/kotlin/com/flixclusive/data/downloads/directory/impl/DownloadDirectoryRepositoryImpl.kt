package com.flixclusive.data.downloads.directory.impl

import android.content.Context
import androidx.core.net.toUri
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DownloadDirectoryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DownloadDirectoryRepository {
    override fun getOrCreateMediaDirectory(
        root: UniFile,
        mediaId: String,
        mediaTitle: String,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ): UniFile? {
        val downloadsDir = root.getOrCreateChildDirectory(DOWNLOADS_FOLDER_NAME) ?: return null
        val mediaDir =
            downloadsDir.getOrCreateChildDirectory(DownloadPathUtil.buildMediaFolderName(mediaId, mediaTitle))
                ?: return null

        if (seasonNumber == null || episodeNumber == null) return mediaDir

        return mediaDir.getOrCreateChildDirectory(DownloadPathUtil.buildEpisodeFolderName(seasonNumber, episodeNumber))
    }

    override fun getOrCreateSubtitlesDirectory(mediaDirectory: UniFile): UniFile? =
        mediaDirectory.getOrCreateChildDirectory(SUBTITLES_FOLDER_NAME)

    override fun getOrCreateFile(
        directory: UniFile,
        fileName: String,
    ): UniFile? = directory.findFile(fileName) ?: directory.createFile(fileName)

    override fun resolveFile(uri: String): UniFile? = UniFile.fromUri(context, uri.toUri())?.takeIf { it.exists() }

    override fun listSubtitleFiles(streamFile: UniFile): List<UniFile> {
        val parent = streamFile.parentFile ?: return emptyList()
        val subtitlesDirectory = parent.findFile(SUBTITLES_FOLDER_NAME) ?: return emptyList()

        return subtitlesDirectory.listFiles()?.toList() ?: emptyList()
    }

    private fun UniFile.getOrCreateChildDirectory(name: String): UniFile? = findFile(name) ?: createDirectory(name)

    companion object {
        private const val DOWNLOADS_FOLDER_NAME = "Downloads"
        private const val SUBTITLES_FOLDER_NAME = "subtitles"
    }
}
