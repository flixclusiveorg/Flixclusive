package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.entities.WatchlistItem

interface WatchlistItemManagerUseCase {
    suspend fun isInWatchlist(id: Int): Boolean
    suspend fun toggleWatchlistStatus(watchlistItem: WatchlistItem): Boolean
}