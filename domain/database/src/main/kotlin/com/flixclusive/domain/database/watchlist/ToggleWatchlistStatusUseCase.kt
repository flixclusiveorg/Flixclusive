package com.flixclusive.domain.database.watchlist

import com.flixclusive.core.database.entity.WatchlistItem
import com.flixclusive.data.database.repository.WatchlistRepository
import javax.inject.Inject

class ToggleWatchlistStatusUseCase @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) {
    suspend fun isInWatchlist(id: String, ownerId: Int): Boolean =
        watchlistRepository.getWatchlistItemById(id, ownerId) != null

    suspend operator fun invoke(watchlistItem: WatchlistItem, ownerId: Int): Boolean {
        val isInWatchlist = isInWatchlist(
            id = watchlistItem.id,
            ownerId = ownerId
        )

        if (isInWatchlist) {
            watchlistRepository.removeById(
                itemId = watchlistItem.id,
                ownerId = ownerId
            )
        } else {
            watchlistRepository.insert(watchlistItem)
        }

        return !isInWatchlist
    }
}
