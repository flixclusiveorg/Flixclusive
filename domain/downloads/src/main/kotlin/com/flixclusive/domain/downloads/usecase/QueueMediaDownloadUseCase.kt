package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

interface QueueMediaDownloadUseCase {
    suspend operator fun invoke(
        media: MediaMetadata,
        episode: Episode?,
        streams: List<Stream>,
        subtitle: Subtitle?,
    ): Async<Long>
}
