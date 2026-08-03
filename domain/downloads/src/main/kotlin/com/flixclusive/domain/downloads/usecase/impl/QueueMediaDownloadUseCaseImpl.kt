package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import javax.inject.Inject

internal class QueueMediaDownloadUseCaseImpl @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
) : QueueMediaDownloadUseCase {
    override suspend fun invoke(
        media: MediaMetadata,
        episode: Episode?,
        ownerId: String,
    ): Async<String> {
        val item = DownloadItem(
            ownerId = ownerId,
            mediaId = media.id,
            mediaTitle = media.title,
            mediaType = media.type,
            seasonNumber = episode?.season,
            episodeNumber = episode?.number,
            state = DownloadItemState.QUEUED,
            sourceUrl = null,
        )

        mediaDownloadRepository.queue(item)
        return Async.Success(item.id)
    }
}
