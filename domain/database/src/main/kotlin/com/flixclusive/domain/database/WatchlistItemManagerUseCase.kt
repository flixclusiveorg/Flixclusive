package com.flixclusive.domain.database

import com.flixclusive.data.watchlist.WatchlistRepository
import com.flixclusive.model.database.WatchlistItem
import javax.inject.Inject

class WatchlistItemManagerUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) {
    suspend fun isInWatchlist(id: Int): Boolean =
        watchlistRepository.getWatchlistItemById(id) != null

    suspend fun toggleWatchlistStatus(watchlistItem: WatchlistItem): Boolean {
        val isInWatchlist = isInWatchlist(id = watchlistItem.id)

        if (isInWatchlist) {
            watchlistRepository.removeById(watchlistItem.id)
        } else {
            watchlistRepository.insert(watchlistItem)
        }

        return !isInWatchlist
    }
}