package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.model.film.Film
import javax.inject.Inject

internal class ToggleWatchlistStatusUseCaseImpl
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
    ) : ToggleWatchlistStatusUseCase {
        override suspend fun invoke(
            watchlist: Watchlist,
            film: Film?,
        ) {
            when (
                watchlistRepository.isInWatchlist(
                    filmId = watchlist.filmId,
                    ownerId = watchlist.ownerId,
                )
            ) {
                true -> watchlistRepository.remove(id = watchlist.id)
                false -> watchlistRepository.insert(item = watchlist, film = film)
            }
        }
    }
