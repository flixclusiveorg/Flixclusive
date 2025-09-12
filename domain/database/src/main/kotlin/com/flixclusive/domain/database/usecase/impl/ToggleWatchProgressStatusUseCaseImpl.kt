package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ToggleWatchProgressStatusUseCaseImpl
    @Inject
    constructor(
        private val watchProgressRepository: WatchProgressRepository,
        private val userSessionManager: UserSessionManager,
    ) : ToggleWatchProgressStatusUseCase {
        override suspend fun invoke(film: Film) {
            val ownerId = userSessionManager.currentUser.first()?.id
            requireNotNull(ownerId) {
                "User must be logged in to toggle watch progress"
            }

            val progress = watchProgressRepository
                .getAsFlow(
                    id = film.identifier,
                    ownerId = ownerId,
                    type = film.filmType,
                ).first()

            when (progress) {
                null -> {
                    watchProgressRepository.insert(
                        item = createWatchProgress(ownerId, film),
                        film = film,
                    )
                }

                else -> {
                    watchProgressRepository.delete(
                        item = progress.id,
                        type = film.filmType,
                    )
                }
            }
        }

        /**
         * Creates a new [WatchProgress] instance for the given [film] and the current user.
         *
         * @param ownerId The ID of the current user.
         * @param film The film for which to create the watch progress.
         *
         * @return A new [WatchProgress] instance with status set to [WatchStatus.COMPLETED].
         *
         * @throws IllegalArgumentException if the [film] is not of type [Movie] or [TvShow].
         * */
        private fun createWatchProgress(
            ownerId: Int,
            film: Film,
        ): WatchProgress {
            return if (film is Movie) {
                MovieProgress(
                    filmId = film.identifier,
                    ownerId = ownerId,
                    progress = 0L,
                    status = WatchStatus.COMPLETED,
                )
            } else if (film is TvShow) {
                // To avoid costly lookups, just set the total episodes as the last watched episode
                EpisodeProgress(
                    filmId = film.identifier,
                    ownerId = ownerId,
                    seasonNumber = film.totalSeasons,
                    episodeNumber = film.totalEpisodes,
                    progress = 0L,
                    status = WatchStatus.COMPLETED,
                )
            } else {
                throw IllegalArgumentException("Unsupported FilmMetadata type for watch progress: $film")
            }
        }
    }
