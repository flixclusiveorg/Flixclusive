package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

data class DownloadableMediaLinks(
    val streams: List<Stream>,
    val subtitle: Subtitle?,
)

/**
 * Resolves the downloadable [Stream]s and preferred [Subtitle] for a media/episode, reusing the
 * same cross-provider resolution and caching as [GetMediaLinksUseCase], then reads the result back
 * from the cache so it can be handed to the download queue.
 */
interface GetDownloadableMediaLinksUseCase {
    suspend operator fun invoke(
        media: MediaMetadata,
        episode: Episode? = null,
    ): Async<DownloadableMediaLinks>
}
