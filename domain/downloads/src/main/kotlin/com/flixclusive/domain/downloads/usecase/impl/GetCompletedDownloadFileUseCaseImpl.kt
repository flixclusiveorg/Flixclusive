package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.CompletedSubtitleFile
import com.flixclusive.domain.downloads.usecase.GetCompletedDownloadFileUseCase
import javax.inject.Inject

internal class GetCompletedDownloadFileUseCaseImpl @Inject constructor(
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
) : GetCompletedDownloadFileUseCase {
    override suspend fun invoke(item: DownloadItem): CompletedDownloadFile? {
        if (item.state != DownloadItemState.COMPLETED) return null
        val streamFilePath = item.streamFilePath ?: return null

        val file = downloadDirectoryRepository.resolveFile(streamFilePath) ?: return null
        val subtitles = downloadDirectoryRepository.listSubtitleFiles(file).map { subtitleFile ->
            CompletedSubtitleFile(
                uri = subtitleFile.uri,
                language = languageFromFileName(subtitleFile.name.orEmpty()),
            )
        }

        return CompletedDownloadFile(
            uri = file.uri,
            mimeType = file.type?.takeIf { it.isNotBlank() } ?: "video/*",
            subtitles = subtitles,
        )
    }

    /** Subtitle files are named `"<title> (<language>).<ext>"` by the download controller — the
     * only place a subtitle's language survives once it's just a file on disk. */
    private fun languageFromFileName(fileName: String): String =
        LANGUAGE_IN_PARENS.find(fileName)?.groupValues?.get(1) ?: fileName.substringBeforeLast('.')

    companion object {
        private val LANGUAGE_IN_PARENS = Regex("""\(([^()]+)\)\.[^.]+$""")
    }
}
