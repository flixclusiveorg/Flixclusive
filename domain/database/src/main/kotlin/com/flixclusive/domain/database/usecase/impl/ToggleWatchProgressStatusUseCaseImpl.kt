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

    private suspend fun invokeForTvShow(ownerId: String, tvShow: TvShow) {
        val progress = watchProgressRepository.get(
            id = tvShow.identifier,
            type = tvShow.filmType,
            ownerId = ownerId,
        )

        if (progress == null) {
            val season = tvShow.seasons
                .lastOrNull()
                ?.number
                ?.takeIf { it == tvShow.totalSeasons }
                ?: tvShow.totalSeasons

            val episode = tvShow.seasons
                .lastOrNull()
                ?.episodes
                ?.lastOrNull()
                ?.number
                ?: tvShow.totalEpisodes

            watchProgressRepository.insert(
                film = tvShow,
                item = EpisodeProgress(
                    filmId = tvShow.identifier,
                    ownerId = ownerId,
                    seasonNumber = season,
                    episodeNumber = episode,
                    status = WatchStatus.COMPLETED,
                    progress = 0L,
                ),
            )

//            tvShow.seasons.forEach { season ->
//                season.episodes.forEach { episode ->
//                    watchProgressRepository.insert(
//                        film = tvShow,
//                        item = EpisodeProgress(
//                            filmId = tvShow.identifier,
//                            ownerId = ownerId,
//                            seasonNumber = season.number,
//                            episodeNumber = episode.number,
//                            status = WatchStatus.COMPLETED,
//                            progress = 0L,
//                        ),
//                    )
//                }
//            }
        } else {
            for (i in 1..tvShow.totalSeasons) {
                val progressList = watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    ownerId = ownerId,
                    seasonNumber = i,
                )

                progressList.forEach { episodeProgress ->
                    watchProgressRepository.delete(
                        item = episodeProgress.id,
                        type = tvShow.filmType,
                    )
                }
            }

        }
    }

    private suspend fun invokeForMovie(ownerId: String, film: Movie) {
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
