package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.providers.models.common.VideoData
import kotlinx.coroutines.flow.Flow

interface VideoDataProviderUseCase {
    val providers: List<String>

    operator fun invoke(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        server: String? = null,
        source: String? = null,
        mediaId: String? = null,
        episode: TMDBEpisode? = null,
        onSuccess: (VideoData, TMDBEpisode?) -> Unit,
        onError: (() -> Unit)? = null,
    ): Flow<VideoDataDialogState>
}