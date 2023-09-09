package com.flixclusive.domain.usecase

import com.flixclusive_provider.models.common.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.VideoDataDialogState
import kotlinx.coroutines.flow.Flow

interface VideoDataProviderUseCase {
    operator fun invoke(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        server: String?,
        mediaId: String? = null,
        episode: TMDBEpisode? = null,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit
    ): Flow<VideoDataDialogState>
}