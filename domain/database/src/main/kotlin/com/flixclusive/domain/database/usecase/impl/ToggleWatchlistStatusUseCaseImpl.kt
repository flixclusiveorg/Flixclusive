package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.model.film.Film
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ToggleWatchlistStatusUseCaseImpl
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
        private val userSessionManager: UserSessionManager
    ) : ToggleWatchlistStatusUseCase {
        override suspend fun invoke(film: Film) {
            val ownerId = userSessionManager.currentUser.first()?.id
            requireNotNull(ownerId) {
                "User must be logged in to toggle watch progress"
            }

            when (
                val item = watchlistRepository.get(
                    filmId = film.identifier,
                    ownerId = ownerId,
                )
            ) {
                null -> watchlistRepository.insert(item = createWatchlist(ownerId, film), film = film)
                else -> watchlistRepository.remove(id = item.id)
            }
        }

        private fun createWatchlist(ownerId: Int, film: Film): Watchlist {
            return Watchlist(
                id = 0,
                filmId = film.identifier,
                ownerId = ownerId
            )
        }
    }
