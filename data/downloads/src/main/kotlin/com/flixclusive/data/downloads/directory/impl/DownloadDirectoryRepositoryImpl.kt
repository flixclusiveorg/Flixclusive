package com.flixclusive.data.downloads.directory.impl

import android.content.Context
import androidx.core.net.toUri
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.flixclusive.core.util.log.errorLog

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

    /**
     * Null for a URI that no longer points at a file we can reach — the user deleted it, the volume
     * went away, or the folder grant was revoked between sessions. A revoked grant surfaces as a
     * [SecurityException] from the resolver rather than a missing file, and left unhandled that
     * would propagate out of a resume as a crash instead of a recoverable "start over".
     */
    override fun resolveFile(uri: String): UniFile? =
        try {
            UniFile.fromUri(context, uri.toUri())?.takeIf { it.exists() }
        } catch (e: SecurityException) {
            errorLog("Lost access to $uri: ${e.message}")
            null
        }

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
