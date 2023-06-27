package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode

interface WatchHistoryItemManagerUseCase {
    suspend fun updateEpisodeCount(
        id: Int,
        seasonNumber: Int,
        episodeCount: Int,
    ): WatchHistoryItem?

    suspend fun updateWatchHistoryItem(
        watchHistoryItem: WatchHistoryItem,
        currentTime: Long,
        totalDuration: Long,
        currentSelectedEpisode: TMDBEpisode? = null,
    ): WatchHistoryItem
}