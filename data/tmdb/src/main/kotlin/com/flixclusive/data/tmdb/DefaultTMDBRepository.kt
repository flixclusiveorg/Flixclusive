package com.flixclusive.data.tmdb

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getMediaTypeFromInt
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.filterOutUnreleasedFilms
import retrofit2.HttpException
import javax.inject.Inject

internal class DefaultTMDBRepository @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val configurationProvider: AppConfigurationManager
) : TMDBRepository {
    override val tmdbApiKey: String
        get() = configurationProvider.appConfig!!.tmdbApiKey

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withIOContext {
            try {
                val response = tmdbApiService.getTrending(
                    mediaType = mediaType,
                    timeWindow = timeWindow,
                    apiKey = tmdbApiKey,
                    page = page
                )

                Resource.Success(response)
            } catch (e: Exception) {
                e.toNetworkException()
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
        return withIOContext {
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
                e.toNetworkException()
            }
        }
    }

    override suspend fun search(
        query: String,
        page: Int,
        filter: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withIOContext {
            try {
                if (query.isEmpty()) {
                    return@withIOContext Resource.Failure("Search query should not be empty!")
                }

                val response = tmdbApiService.search(
                    mediaType = getMediaTypeFromInt(filter),
                    apiKey = tmdbApiKey,
                    page = page,
                    query = query
                )

                Resource.Success(response)
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }

    override suspend fun getLogo(
        mediaType: String,
        id: Int,
    ): Resource<String> {
        return withIOContext {
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
                e.toNetworkException()
            }
        }
    }

    override suspend fun getMovie(id: Int): Resource<Movie> {
        return withIOContext {
            try {
                val movie = tmdbApiService.getMovie(
                    id = id, apiKey = tmdbApiKey
                )

                val collection: TMDBCollection? = if (movie.collection != null) {
                    val collection = getCollection(id = movie.collection!!.id)

                    if (collection !is Resource.Success)
                        throw Exception("Error fetching collection of ${movie.title} [${movie.id}]")

                    collection.data?.also { data ->
                        data.films.sortedWith(nullsLast(compareBy { it.year }))
                    }
                } else null

                val newGenres = formatGenreIds(
                    genreIds = movie.genres.map { it.id },
                    genresList = configurationProvider
                        .searchCatalogsData!!.genres.map {
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
                e.toNetworkException()
            }
        }
    }

    override suspend fun getTvShow(id: Int): Resource<TvShow> {
        return withIOContext {
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
                        .searchCatalogsData!!.genres.map {
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
                e.toNetworkException()
            }
        }
    }

    override suspend fun getSeason(id: Int, seasonNumber: Int): Resource<Season> {
        return withIOContext {
            try {
                val season = tmdbApiService.getSeason(
                    id = id, seasonNumber = seasonNumber, apiKey = tmdbApiKey
                )

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
            withIOContext {
                val season = getSeason(id, seasonNumber)

                if (season is Resource.Failure)
                    return@withIOContext Resource.Failure(season.error)

                val episodeId = season.data!!.episodes.find {
                    it.season == seasonNumber
                            && it.number == episodeNumber
                }
                Resource.Success(episodeId)
            }
        } catch (e: Exception) {
            e.toNetworkException()
        }
    }

    override suspend fun getCollection(id: Int): Resource<TMDBCollection> {
        return withIOContext {
            try {
                val response = tmdbApiService.getCollection(
                    id = id, apiKey = tmdbApiKey
                )

                Resource.Success(response)
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }

    override suspend fun paginateConfigItems(
        url: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withIOContext {
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

    private fun List<Season>.filterOutUnreleasedSeasons()
        = filterNot { it.isUnreleased }

    private fun List<Season>.filterOutZeroSeasons()
        = filterNot { it.number == 0 }

    private fun formatGenreIds(
        genreIds: List<Int>,
        genresList: List<Genre>
    ): List<Genre> {
        val genreMap = genresList.associateBy({ it.id }, { it })
        return genreIds.mapNotNull { genreMap[it] }
    }
}