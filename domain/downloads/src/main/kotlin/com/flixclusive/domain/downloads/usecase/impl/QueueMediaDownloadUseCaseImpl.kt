package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.downloads.DownloadStreamCandidate
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.domain.downloads.usecase.ResolveDownloadableStreamUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import javax.inject.Inject

internal class QueueMediaDownloadUseCaseImpl @Inject constructor(
    private val resolveDownloadableStreamUseCase: ResolveDownloadableStreamUseCase,
    private val mediaDownloadRepository: MediaDownloadRepository,
) : QueueMediaDownloadUseCase {
    override suspend fun invoke(
        media: MediaMetadata,
        episode: Episode?,
        streams: List<Stream>,
        subtitle: Subtitle?,
    ): Async<Long> {
        val resolved = resolveDownloadableStreamUseCase(streams)
        if (resolved is Async.Failure) return resolved

        val (stream, fallbacks) = (resolved as Async.Success).data
        val item = DownloadItem(
            mediaId = media.id,
            mediaTitle = media.title,
            mediaType = media.type,
            seasonNumber = episode?.season,
            episodeNumber = episode?.number,
            episodeTitle = episode?.title,
            state = DownloadItemState.QUEUED,
            streamUrl = stream.url,
            streamHeaders = stream.customHeaders,
            streamFallbackCandidates = fallbacks
                .map { candidate ->
                    DownloadStreamCandidate(url = candidate.url, headers = candidate.customHeaders)
                }.ifEmpty { null },
            subtitleUrl = subtitle?.url,
            subtitleHeaders = subtitle?.customHeaders,
        )

        val id = mediaDownloadRepository.queue(item)
        return Async.Success(id)
    }
}
