package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.util.DownloadPathUtil
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.GetCompletedDownloadFileUseCase
import com.flixclusive.domain.downloads.usecase.GetDownloadDirectoryUseCase
import javax.inject.Inject

internal class GetCompletedDownloadFileUseCaseImpl @Inject constructor(
    private val getDownloadDirectoryUseCase: GetDownloadDirectoryUseCase,
    private val downloadDirectoryRepository: DownloadDirectoryRepository,
) : GetCompletedDownloadFileUseCase {
    override suspend fun invoke(item: DownloadItem): CompletedDownloadFile? {
        if (item.state != DownloadItemState.COMPLETED) return null
        val streamUrl = item.streamUrl ?: return null

        val directory = getDownloadDirectoryUseCase(
            mediaId = item.mediaId,
            mediaTitle = item.mediaTitle,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
        ) ?: return null

        val fileName = DownloadPathUtil.buildStreamFileName(
            DownloadPathUtil.buildFileTitle(item.mediaTitle, item.episodeTitle),
            DownloadPathUtil.extensionFromUrl(
                streamUrl,
                DownloadPathUtil.DEFAULT_STREAM_EXTENSION,
                DownloadPathUtil.STREAM_EXTENSIONS
            ),
        )

        val file = downloadDirectoryRepository.getOrCreateFile(directory, fileName) ?: return null

        return CompletedDownloadFile(
            uri = file.uri,
            mimeType = file.type?.takeIf { it.isNotBlank() } ?: "video/*",
        )
    }
}
