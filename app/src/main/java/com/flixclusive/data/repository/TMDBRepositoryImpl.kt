package com.flixclusive.data.repository

import com.flixclusive.common.Constants.TMDB_API_BASE_URL
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.dto.tmdb.common.TMDBImagesResponseDto
import com.flixclusive.data.dto.tmdb.toMovie
import com.flixclusive.data.dto.tmdb.toTvShow
import com.flixclusive.data.dto.tmdb.tv.toSeason
import com.flixclusive.data.utils.catchInternetRelatedException
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBCollection
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.utils.TMDBUtils.filterOutUnreleasedRecommendations
import com.flixclusive.domain.utils.TMDBUtils.filterOutZeroSeasons
import com.flixclusive.presentation.utils.FormatterUtils.formatGenreIds
import com.flixclusive.utils.LoggerUtils.errorLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class TMDBRepositoryImpl @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val configurationProvider: ConfigurationProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TMDBRepository {
    override val tmdbApiKey: String
        get() = configurationProvider.appConfig!!.tmdbApiKey

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int,
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
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>?,
        withCompanies: List<Int>?,
        withGenres: List<Genre>?,
        sortBy: SortOptions,
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
        return withContext(ioDispatcher) {
            try {
                val sortOption = when (sortBy) {
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
                e.catchInternetRelatedException()
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
                if (query.isEmpty()) {
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
                e.catchInternetRelatedException()
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
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun getMovie(id: Int): Resource<Movie> {
        return withContext(ioDispatcher) {
            try {
                val movie = tmdbApiService.getMovie(
                    id = id, apiKey = tmdbApiKey
                )

                val collection: TMDBCollection? = if (movie.belongsToCollection != null) {
                    val collection = getCollection(id = movie.belongsToCollection.id)

                    if (collection !is Resource.Success)
                        throw Exception("Error fetching collection of ${movie.title} [${movie.id}]")

                    collection.data
                } else null

                val filteredRecommendations =
                    filterOutUnreleasedRecommendations(movie.recommendations.results)
                val newGenres = formatGenreIds(
                    genreIds = movie.genres.map { it.id },
                    genresList = configurationProvider
                        .searchCategoriesConfig!!.genres.map {
                            Genre(
                                id = it.id,
                                name = it.name,
                                mediaType = it.mediaType
                            )
                        }
                )

                Resource.Success(
                    movie.copy(
                        recommendations = movie.recommendations.copy(
                            results = filteredRecommendations
                        ),
                        genres = newGenres
                    )
                        .toMovie()
                        .copy(
                            collection = collection?.copy(
                                films = filterOutUnreleasedRecommendations(collection.films)
                            )
                        )
                )
            } catch (e: Exception) {
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun getTvShow(id: Int): Resource<TvShow> {
        return withContext(ioDispatcher) {
            try {
                val tvShow = tmdbApiService.getTvShow(
                    id = id, apiKey = tmdbApiKey
                )

                val filteredRecommendations =
                    filterOutUnreleasedRecommendations(tvShow.recommendations.results)
                val filteredSeasons = filterOutZeroSeasons(tvShow.seasons)
                val newGenres = formatGenreIds(
                    genreIds = tvShow.genres.map { it.id },
                    genresList = configurationProvider
                        .searchCategoriesConfig!!.genres.map {
                            Genre(
                                id = it.id,
                                name = it.name,
                                mediaType = it.mediaType
                            )
                        }
                )

                Resource.Success(
                    tvShow.copy(
                        seasons = filteredSeasons,
                        numberOfSeasons = filteredSeasons.size,
                        recommendations = tvShow.recommendations.copy(
                            results = filteredRecommendations
                        ),
                        genres = newGenres
                    ).toTvShow()
                )
            } catch (e: Exception) {
                e.catchInternetRelatedException()
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
                    season.toSeason()
                )
            } catch (e: Exception) {
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<TMDBEpisode?> {
        return withContext(ioDispatcher) {
            val season = getSeason(id, seasonNumber)

            if (season is Resource.Failure)
                return@withContext Resource.Failure(season.error)

            val episodeId = season.data!!.episodes.find {
                it.season == seasonNumber
                        && it.episode == episodeNumber
            }
            Resource.Success(episodeId)
        }
    }

    override suspend fun getCollection(id: Int): Resource<TMDBCollection> {
        return withContext(ioDispatcher) {
            try {
                val response = tmdbApiService.getCollection(
                    id = id, apiKey = tmdbApiKey
                )

                Resource.Success(response)
            } catch (e: Exception) {
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun paginateConfigItems(
        url: String,
        page: Int,
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
        return withContext(ioDispatcher) {
            val fullUrl = "$TMDB_API_BASE_URL$url&page=$page&api_key=$tmdbApiKey"

            try {
                Resource.Success(tmdbApiService.get(fullUrl))
            } catch (e: HttpException) {
                errorLog("Http Error (${e.code()}) on URL[$fullUrl]: ${e.response()}")
                errorLog(e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }
}