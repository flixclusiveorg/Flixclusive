package com.flixclusive.domain.provider.usecase.tracker

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.tracker.ScrobbleAction
import kotlinx.coroutines.flow.Flow

interface SyncToScrobblersUseCase {
    operator fun invoke(
        action: ScrobbleAction,
        watchProgress: WatchProgress,
        media: MediaMetadata,
        episode: Episode? = null,
    ): Flow<Async<Unit>>
}
