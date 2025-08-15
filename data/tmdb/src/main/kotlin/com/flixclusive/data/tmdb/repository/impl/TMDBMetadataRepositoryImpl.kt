package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class TMDBMetadataRepositoryImpl @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val appDispatchers: AppDispatchers,
) : TMDBMetadataRepository {
    override suspend fun getMovie(id: Int): Resource<Movie> {
        return withContext(appDispatchers.io) {
            try {
                val movie = tmdbApiService.getMovie(id = id)

                Resource.Success(movie)
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }

    override suspend fun getTvShow(id: Int): Resource<TvShow> {
        return withContext(appDispatchers.io) {
            try {
                val tvShow = tmdbApiService.getTvShow(id = id)

                val filteredSeasons = tvShow.seasons
                    .filterOutZeroSeasons()
                    .filterOutUnreleasedSeasons()

                Resource.Success(
                    tvShow.copy(
                        seasons = filteredSeasons,
                        totalSeasons = filteredSeasons.size,
                    ),
                )
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }

    override suspend fun getSeason(
        id: Int,
        seasonNumber: Int,
    ): Resource<Season> {
        return withContext(appDispatchers.io) {
            try {
                val season = tmdbApiService.getSeason(id = id, seasonNumber = seasonNumber)

                Resource.Success(season)
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }

    override suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?> {
        return try {
            withContext(appDispatchers.io) {
                val season = getSeason(id, seasonNumber)

                if (season is Resource.Failure) {
                    return@withContext Resource.Failure(season.error)
                }

                val episodeId = season.data!!.episodes.find {
                    it.season == seasonNumber &&
                        it.number == episodeNumber
                }
                Resource.Success(episodeId)
            }
        } catch (e: Exception) {
            e.toNetworkException()
        }
    }

    private fun List<Season>.filterOutUnreleasedSeasons() = filterNot { it.isUnreleased }

    private fun List<Season>.filterOutZeroSeasons() = filterNot { it.number == 0 }
}
