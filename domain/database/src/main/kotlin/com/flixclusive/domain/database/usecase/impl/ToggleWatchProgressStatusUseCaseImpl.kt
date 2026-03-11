package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ToggleWatchProgressStatusUseCaseImpl @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionManager: UserSessionManager,
) : ToggleWatchProgressStatusUseCase {
    override suspend fun invoke(film: Film) {
        val ownerId = userSessionManager.currentUser.first()?.id
        requireNotNull(ownerId) {
            "User must be logged in to toggle watch progress"
        }

        when (film) {
            is Movie -> invokeForMovie(ownerId, film)
            is TvShow -> invokeForTvShow(ownerId, film)
        }
    }

    private suspend fun invokeForTvShow(ownerId: Int, film: TvShow) {
        val progress = watchProgressRepository.get(
            id = film.identifier,
            ownerId = ownerId,
            type = film.filmType,
        )

        if (progress == null || (progress.watchData as EpisodeProgress).episodeNumber < film.totalEpisodes) {
            watchProgressRepository.insert(
                film = film,
                item = EpisodeProgress(
                    filmId = film.identifier,
                    ownerId = ownerId,
                    seasonNumber = film.totalSeasons,
                    episodeNumber = film.totalEpisodes,
                    progress = 0L,
                    status = WatchStatus.COMPLETED,
                ),
            )
        } else {
            for (i in 1..film.totalSeasons) {
                val progressList = watchProgressRepository.getSeasonProgress(
                    tvShowId = film.identifier,
                    ownerId = ownerId,
                    seasonNumber = i,
                )

                progressList.forEach { episodeProgress ->
                    watchProgressRepository.delete(
                        item = episodeProgress.id,
                        type = film.filmType,
                    )
                }
            }

        }
    }

    private suspend fun invokeForMovie(ownerId: Int, film: Movie) {
        val progress = watchProgressRepository.get(
            id = film.identifier,
            ownerId = ownerId,
            type = film.filmType,
        )

        if (progress == null) {
            watchProgressRepository.insert(
                film = film,
                item = MovieProgress(
                    filmId = film.identifier,
                    ownerId = ownerId,
                    progress = 0L,
                    status = WatchStatus.COMPLETED,
                ),
            )
        } else {
            watchProgressRepository.delete(
                item = progress.id,
                type = film.filmType,
            )
        }
    }
}
