package com.flixclusive.data.repository

import android.util.Log
import com.flixclusive.common.Constants.FLIXCLUSIVE_LOG_TAG
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.data.dto.tmdb.common.toList
import com.flixclusive.data.dto.tmdb.toMovie
import com.flixclusive.data.dto.tmdb.toTvShow
import com.flixclusive.data.dto.tmdb.tv.toSeason
import com.flixclusive.data.utils.TMDBUtils.filterOutUnreleasedRecommendations
import com.flixclusive.data.utils.TMDBUtils.filterOutZeroSeasons
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.firebase.ConfigurationProvider
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TMDBRepositoryImpl @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val configurationProvider: ConfigurationProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TMDBRepository  {
    override val tmdbApiKey: String
        get() = configurationProvider.tmdbApiKey

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
        return withContext(ioDispatcher) {
            try {
                val response = tmdbApiService.getTrending(
                    mediaType = mediaType,
                    timeWindow = timeWindow,
                    apiKey = tmdbApiKey,
                    page = page
                )

                Resource.Success(response)
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>?,
        withCompanies: List<Int>?,
        withGenres: List<Genre>?,
        sortBy: SortOptions
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
        return withContext(ioDispatcher) {
            try {
                val sortOption = when(sortBy) {
                    SortOptions.POPULARITY -> "popularity.desc"
                    SortOptions.RATED -> "vote_average.desc"
                }

                val response = tmdbApiService.discoverFilms(
                    mediaType = mediaType,
                    apiKey = tmdbApiKey,
                    page = page,
                    sortBy = sortOption,
                    networks = withNetworks?.joinToString(",") ?: "",
                    companies = withCompanies?.joinToString(",") ?: "",
                    genres = withGenres?.joinToString(",") { it.id.toString() } ?: ""
                )

                Resource.Success(response)
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getGenres(mediaType: String): Resource<List<Genre>> {
        return withContext(ioDispatcher) {
            try {
                val result = tmdbApiService.getGenres(
                    mediaType = mediaType,
                    apiKey = tmdbApiKey,
                )

                Resource.Success(result.toList())
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun search(
        mediaType: String,
        query: String,
        page: Int,
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
        return withContext(ioDispatcher) {
            try {
                if(query.isEmpty()) {
                    Resource.Failure("Search query should not be empty!")
                }

                val response = tmdbApiService.search(
                    mediaType = mediaType,
                    apiKey = tmdbApiKey,
                    page = page,
                    query = query
                )

                Resource.Success(response)
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getImages(
        mediaType: String,
        id: Int,
    ): Resource<TMDBImagesResponseDto> {
        return withContext(ioDispatcher) {
            try {
                val response = tmdbApiService.getImages(
                    mediaType = mediaType,
                    apiKey = tmdbApiKey,
                    id = id
                )

                Resource.Success(response)
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getMovie(id: Int): Resource<Movie> {
        return withContext(ioDispatcher) {
            try {
                val movie = tmdbApiService.getMovie(
                    id = id, apiKey = tmdbApiKey
                )

                val filteredRecommendations = filterOutUnreleasedRecommendations(movie.recommendations.results)
                Resource.Success(
                    movie.copy(
                        recommendations = movie.recommendations.copy(
                            results = filteredRecommendations
                        )
                    )
                    .toMovie()
                )
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getTvShow(id: Int): Resource<TvShow> {
        return withContext(ioDispatcher) {
            try {
                val tvShow = tmdbApiService.getTvShow(
                    id = id, apiKey = tmdbApiKey
                )

                val filteredRecommendations = filterOutUnreleasedRecommendations(tvShow.recommendations.results)
                val filteredSeasons = filterOutZeroSeasons(tvShow.seasons)
                Resource.Success(
                    tvShow.copy(
                        seasons = filteredSeasons,
                        numberOfSeasons = filteredSeasons.size,
                        recommendations = tvShow.recommendations.copy(
                            results = filteredRecommendations
                        )
                    ).toTvShow()
                )
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getSeason(id: Int, seasonNumber: Int): Resource<Season> {
        return withContext(ioDispatcher) {
            try {
                val season = tmdbApiService.getSeason(
                    id = id, seasonNumber = seasonNumber, apiKey = tmdbApiKey
                )

                Resource.Success(
                    season
                        .toSeason()
                )
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ): TMDBEpisode? {
        return withContext(ioDispatcher) {
            val season = getSeason(id, seasonNumber)

            if (season !is Resource.Success)
                return@withContext null

            season.data!!.episodes.find {
                it.season == seasonNumber
                    && it.episode == episodeNumber
            }
        }
    }
}