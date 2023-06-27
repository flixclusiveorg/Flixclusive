package com.flixclusive.data.usecase

import com.flixclusive.domain.model.entities.WatchlistItem
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.WatchlistItemManagerUseCase
import javax.inject.Inject

class WatchlistItemManagerUseCaseImpl @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
) : WatchlistItemManagerUseCase {
    override suspend fun isInWatchlist(id: Int): Boolean =
        watchlistRepository.getWatchlistItemById(id) != null

    override suspend fun toggleWatchlistStatus(watchlistItem: WatchlistItem): Boolean {
        val isInWatchlist = isInWatchlist(id = watchlistItem.id)

        if (isInWatchlist) {
            watchlistRepository.removeById(watchlistItem.id)
        } else {
            watchlistRepository.insert(watchlistItem)
        }

        return !isInWatchlist
    }
}