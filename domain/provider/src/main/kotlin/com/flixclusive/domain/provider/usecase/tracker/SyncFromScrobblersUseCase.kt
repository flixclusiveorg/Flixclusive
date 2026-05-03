package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface SyncFromScrobblersUseCase {
    suspend operator fun invoke(
        item: MediaMetadata,
        episode: Episode? = null,
    )
}
