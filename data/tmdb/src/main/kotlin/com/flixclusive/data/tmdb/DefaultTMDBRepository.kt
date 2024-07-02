package com.flixclusive.data.tmdb

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.TMDBCollection
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.model.tmdb.common.tv.Season
import com.flixclusive.model.tmdb.util.TMDB_API_BASE_URL
import com.flixclusive.model.tmdb.util.filterOutUnreleasedFilms
import com.flixclusive.model.tmdb.util.filterOutUnreleasedSeasons
import com.flixclusive.model.tmdb.util.filterOutZeroSeasons
import com.flixclusive.model.tmdb.util.formatGenreIds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

internal class DefaultTMDBRepository @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val configurationProvider: AppConfigurationManager,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TMDBRepository {
    override val tmdbApiKey: String
        get() = configurationProvider.appConfig!!.tmdbApiKey

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
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
    ): Resource<SearchResponseData<FilmSearchItem>> {
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
        query: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withContext(ioDispatcher) {
            try {
                if (query.isEmpty()) {
                    return@withContext Resource.Failure("Search query should not be empty!")
                }

                val response = tmdbApiService.search(
                    mediaType = "multi", // TODO: Dynamically change this again
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

    override suspend fun getLogo(
        mediaType: String,
        id: Int,
    ): Resource<String> {
        return withContext(ioDispatcher) {
            try {
                val response =  tmdbApiService.getImages(
                    mediaType = mediaType,
                    apiKey = tmdbApiKey,
                    id = id
                )

                val logo = response.logos!!.first()
                    .filePath.replace("svg", "png")

                Resource.Success(logo)
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

                val collection: TMDBCollection? = if (movie.collection != null) {
                    val collection = getCollection(id = movie.collection!!.id)

                    if (collection !is Resource.Success)
                        throw Exception("Error fetching collection of ${movie.title} [${movie.id}]")

                    collection.data
                } else null

                val newGenres = formatGenreIds(
                    genreIds = movie.genres.map { it.id },
                    genresList = configurationProvider
                        .searchCategoriesData!!.genres.map {
                            Genre(
                                id = it.id,
                                name = it.name,
                                mediaType = it.mediaType
                            )
                        }
                )

                Resource.Success(
                    movie.copy(
                        genres = newGenres,
                        collection = collection?.run {
                            copy(films = films.filterOutUnreleasedFilms())
                            }
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

                val filteredSeasons = tvShow.seasons
                    .filterOutZeroSeasons()
                    .filterOutUnreleasedSeasons()

                val newGenres = formatGenreIds(
                    genreIds = tvShow.genres.map { it.id },
                    genresList = configurationProvider
                        .searchCategoriesData!!.genres.map {
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
                        totalSeasons = filteredSeasons.size,
                        genres = newGenres
                    )
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

                Resource.Success(season)
            } catch (e: Exception) {
                e.catchInternetRelatedException()
            }
        }
    }

    override suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?> {
        return try {
            withContext(ioDispatcher) {
                val season = getSeason(id, seasonNumber)

                if (season is Resource.Failure)
                    return@withContext Resource.Failure(season.error)

                val episodeId = season.data!!.episodes.find {
                    it.season == seasonNumber
                            && it.number == episodeNumber
                }
                Resource.Success(episodeId)
            }
        } catch (e: Exception) {
            e.catchInternetRelatedException()
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
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withContext(ioDispatcher) {
            val fullUrl = "$TMDB_API_BASE_URL$url&page=$page&api_key=$tmdbApiKey"

            try {
                val response = tmdbApiService.get(fullUrl)
                Resource.Success(response)
            } catch (e: HttpException) {
                errorLog("Http Error (${e.code()}) on URL[$fullUrl]: ${e.response()}")
                errorLog(e)
                Resource.Failure(e.actualMessage)
            } catch (e: Exception) {
                errorLog(e)
                Resource.Failure(e.actualMessage)
            }
        }
    }
}