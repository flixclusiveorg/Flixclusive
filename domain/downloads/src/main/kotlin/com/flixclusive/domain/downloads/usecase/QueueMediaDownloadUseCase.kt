package com.flixclusive.domain.downloads.usecase

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface QueueMediaDownloadUseCase {
    suspend operator fun invoke(
        media: MediaMetadata,
        episode: Episode?,
        ownerId: String,
    ): Async<String>
}
