package com.flixclusive.domain.database.usecase.impl

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ToggleWatchProgressStatusUseCaseImpl @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionDataStore: UserSessionDataStore,
) : ToggleWatchProgressStatusUseCase {
    override suspend fun invoke(film: Film) {
        val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

        when (film) {
            is Movie -> invokeForMovie(ownerId, film)
            is TvShow -> invokeForTvShow(ownerId, film)
        }
    }

    private suspend fun invokeForTvShow(ownerId: String, tvShow: TvShow) {
        val progress = watchProgressRepository.get(
            id = tvShow.id,
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
                    filmId = tvShow.id,
                    ownerId = ownerId,
                    seasonNumber = season,
                    episodeNumber = episode,
                    status = WatchStatus.COMPLETED,
                    progress = 0L,
                ),
            )
        } else {
            for (i in 1..tvShow.totalSeasons) {
                val progressList = watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.id,
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
            id = film.id,
            ownerId = ownerId,
            type = film.filmType,
        )

        if (progress == null) {
            watchProgressRepository.insert(
                film = film,
                item = MovieProgress(
                    filmId = film.id,
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
