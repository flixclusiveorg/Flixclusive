package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.TvShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class GetSeasonWithWatchProgressUseCaseImpl
    @Inject
    constructor(
        private val tmdbMetadataRepository: TMDBMetadataRepository,
        private val watchProgressRepository: WatchProgressRepository,
        private val userSessionManager: UserSessionManager,
    ) : GetSeasonWithWatchProgressUseCase {
        override fun invoke(
            tvShow: TvShow,
            number: Int,
        ): Flow<Resource<SeasonWithProgress>> =
            flow {
                emit(Resource.Loading)

                // Try to get the season from the TvShow.seasons property first
                var season = tvShow.seasons.find { it.number == number }

                if ((season == null || season.episodes.isEmpty()) && tvShow.isFromTmdbSource) {
                    val tmdbSeason = tmdbMetadataRepository.getSeason(
                        id = tvShow.tmdbId!!,
                        seasonNumber = number,
                    )

                    if (tmdbSeason is Resource.Success) {
                        season = tmdbSeason.data
                    } else if (tmdbSeason is Resource.Failure) {
                        emit(Resource.Failure(tmdbSeason.error))
                        return@flow
                    }
                }

                if (season == null) {
                    emit(Resource.Failure(UiText.from(R.string.failed_to_fetch_season_message, number)))
                    return@flow
                }

                val user = userSessionManager.currentUser.value!!
                val progressList = watchProgressRepository.getSeasonProgress(
                    tvShowId = tvShow.identifier,
                    seasonNumber = number,
                    ownerId = user.id,
                )

                val episodes = season.episodes.map { episode ->
                    val episodeIndex = progressList.binarySearchBy(episode.number) { it.episodeNumber }

                    EpisodeWithProgress(
                        episode = episode,
                        watchProgress = progressList.getOrNull(episodeIndex),
                    )
                }

                return@flow emit(Resource.Success(SeasonWithProgress(season = season, episodes = episodes)))
            }
    }

/**
 * Checks if the TvShow is from TMDB source by verifying if it has a non-null tmdbId
 * and if the providerId matches the default film source name (case-insensitive).
 *
 * TODO: Update core-stubs's [TvShow.isFromTmdb] so no need to redefine this here.
 * */
private val TvShow.isFromTmdbSource: Boolean
    get() = tmdbId != null && providerId.equals(DEFAULT_FILM_SOURCE_NAME, ignoreCase = true)
