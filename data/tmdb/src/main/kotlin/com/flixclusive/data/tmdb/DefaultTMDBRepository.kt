package com.flixclusive.data.tmdb

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.jsoup.asJsoup
import com.flixclusive.core.util.network.okhttp.request
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
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.HttpException
import java.net.URLDecoder
import javax.inject.Inject

const val TMDB_API_KEY: String = "8d6d91941230817f7807d643736e8a49"

internal class DefaultTMDBRepository @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val okHttpClient: OkHttpClient,
    private val configurationProvider: AppConfigurationManager
) : TMDBRepository {
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
                    apiKey = TMDB_API_KEY,
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
                    apiKey = TMDB_API_KEY,
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
                    apiKey = TMDB_API_KEY,
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
                    apiKey = TMDB_API_KEY,
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
                    id = id, apiKey = TMDB_API_KEY
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
                    id = id, apiKey = TMDB_API_KEY
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
                    id = id, seasonNumber = seasonNumber, apiKey = TMDB_API_KEY
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
                    id = id, apiKey = TMDB_API_KEY
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
            val fullUrl = "$TMDB_API_BASE_URL$url&page=$page&api_key=$TMDB_API_KEY"

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

    override suspend fun getWatchProviders(
        mediaType: String,
        id: Int
    ): Resource<List<Stream>> {
        require(mediaType == "movie" || mediaType == "tv") {
            "Invalid media type: $mediaType"
        }

        return try {
            val response = withIOContext {
                okHttpClient.request(
                    url = "https://www.themoviedb.org/${mediaType}/${id}/watch?locale=US"
                ).execute()
            }

            withDefaultContext {
                val html = response.asJsoup()
                val streams = parseStreamingInfo(html)
                Resource.Success(streams)
            }
        } catch (e: HttpException) {
            errorLog("Http Error (${e.code()}): ${e.response()}")
            errorLog(e)
            Resource.Failure(e.actualMessage)
        } catch (e: Exception) {
            errorLog(e)
            Resource.Failure(e.actualMessage)
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

    private fun parseStreamingInfo(html: Document): List<Stream> {
        val streamingInfoList = mutableListOf<Stream>()

        html.select("div.ott_provider li a").forEach { element ->
            val href = element.attr("href")
            val title = element.attr("title")
            val logoUrl = element.select("img").attr("src")

            val providerName = title.split(" on ")
                .lastOrNull()
                ?.trim()
                ?: "Unknown Provider"

            // Extract the URL from the 'r' parameter in the href
            val url = href.split("&r=")
                .getOrNull(1)
                ?.split("&")
                ?.firstOrNull()
                ?.let { URLDecoder.decode(it, "UTF-8") }

            if (url != null && !streamingInfoList.any { it.url == url }) {
                streamingInfoList.add(
                    Stream(
                        name = providerName,
                        description = title,
                        url = url,
                        flags = setOf(
                            Flag.Trusted(
                                name = providerName,
                                logo = logoUrl
                            )
                        )
                    )
                )
            }
        }

        return streamingInfoList
    }
}