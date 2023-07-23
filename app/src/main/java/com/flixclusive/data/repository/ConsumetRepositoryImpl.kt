package com.flixclusive.data.repository

import android.util.Log
import com.flixclusive.common.Constants.FLIXCLUSIVE_LOG_TAG
import com.flixclusive.data.api.ConsumetApiService
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.firebase.ConfigurationProvider
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.consumet.VideoDataServer
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType.Companion.toFilmType
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.ConsumetRepository
import com.flixclusive.domain.utils.ConsumetUtils.getConsumetEpisodeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class ConsumetRepositoryImpl @Inject constructor(
    private val consumetApiService: ConsumetApiService,
    private val configurationProvider: ConfigurationProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ConsumetRepository {
    override val consumetDefaultWatchProvider: String
        get() = configurationProvider.consumetDefaultWatchProvider
    override val consumetDefaultVideoServer: String
        get() = configurationProvider.consumetDefaultVideoServer

    override suspend fun getMovieStreamingLinks(
        consumetId: String,
        server: String
    ): Resource<VideoData?> {
        return withContext(ioDispatcher) {
            try {
                val episodeId = consumetId.split("-").last()
                val availableServers = consumetApiService.getAvailableServers(
                    episodeId = episodeId,
                    mediaId = consumetId,
                    provider = consumetDefaultWatchProvider
                )
                var index = availableServers.indexOfFirst { it.serverName == server }

                for (retries in availableServers.indices) {
                    try {
                        if (index >= availableServers.size || index == -1) {
                            // Reset the index if it exceeds the available servers list
                            index = 0
                        }

                        val response = consumetApiService.getStreamingLinks(
                            episodeId = episodeId,
                            mediaId = consumetId,
                            server = availableServers[index].serverName,
                            provider = consumetDefaultWatchProvider
                        )

                        return@withContext Resource.Success(
                            response.copy(episodeId = episodeId, mediaId = consumetId)
                        )
                    } catch (e: HttpException) {
                        index++
                    }
                }

                Resource.Failure("No servers are available!")
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getTvShowStreamingLinks(
        consumetId: String,
        episode: TMDBEpisode,
        server: String
    ): Resource<VideoData?> {
        return withContext(ioDispatcher) {
            try {
                val tvShowInfo = consumetApiService.getTvShow(
                    id = consumetId,
                    provider = consumetDefaultWatchProvider
                )

                val episodeId = getConsumetEpisodeId(
                    episode = episode,
                    listOfEpisode = tvShowInfo.episodes
                ) ?: return@withContext Resource.Success(null)

                val availableServers = consumetApiService.getAvailableServers(
                    episodeId = episodeId,
                    mediaId = consumetId,
                    provider = consumetDefaultWatchProvider
                )
                var index = availableServers.indexOfFirst { it.serverName == server }

                for (retries in availableServers.indices) {
                    try {
                        if (index >= availableServers.size || index == -1) {
                            // Reset the index if it exceeds the available servers list
                            index = 0
                        }

                        val response = consumetApiService.getStreamingLinks(
                            episodeId = episodeId,
                            mediaId = consumetId,
                            server = availableServers[index].serverName,
                            provider = consumetDefaultWatchProvider
                        )

                        return@withContext Resource.Success(
                            response.copy(episodeId = episodeId, mediaId = consumetId)
                        )
                    } catch (e: HttpException) {
                        index++
                    }
                }

                Resource.Failure("No servers are available!")
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun getConsumetFilmMediaId(film: Film?): String? {
        return withContext(ioDispatcher) {
            try {
                if(film == null)
                    return@withContext null

                var i = 1
                var id: String? = null
                val maxPage = 10

                while (id == null) {
                    if (i >= maxPage) {
                        return@withContext ""
                    }

                    val searchResponse = consumetApiService.searchStreamingLinks(
                        query = film.title,
                        page = i,
                        provider = consumetDefaultWatchProvider
                    )

                    if(searchResponse.results.isEmpty())
                        return@withContext ""

                    val matchingResults = searchResponse
                        .results.filter { result ->
                            val titleMatches = result.title.equals(film.title, ignoreCase = true)
                            val filmTypeMatches = result.type.toFilmType() == film.filmType
                            val releaseDateMatches = result.releaseDate == film.dateReleased.split(" ").last()

                            if (titleMatches && filmTypeMatches && film is TvShow) {
                                if(film.seasons.size == result.seasons) {
                                    return@filter true
                                }

                                val tvShowInfo = consumetApiService.getTvShow(
                                    id = result.id,
                                    provider = consumetDefaultWatchProvider
                                )
                                val tvReleaseDateMatches = tvShowInfo.releaseDate.split("-").first() == film.dateReleased.split("-").first()

                                if (tvReleaseDateMatches) {
                                    return@filter true
                                }
                            } else if (titleMatches && filmTypeMatches && releaseDateMatches) {
                                return@filter true
                            }

                            return@filter false
                        }

                    id = matchingResults.firstOrNull()?.id

                    if(searchResponse.hasNextPage) {
                        i++
                    } else if(id.isNullOrEmpty()) {
                        id = ""
                        break
                    }
                }

                id
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                null
            }
        }
    }

    override suspend fun getAvailableServers(
        mediaId: String,
        episodeId: String,
    ): Resource<List<VideoDataServer>> {
        return withContext(ioDispatcher) {
            try {
                val result = consumetApiService.getAvailableServers(
                    episodeId = episodeId,
                    mediaId = mediaId,
                    provider = consumetDefaultWatchProvider
                )
                Resource.Success(result)
            } catch (e: Exception) {
                Log.e(FLIXCLUSIVE_LOG_TAG, e.stackTraceToString())
                Resource.Failure(e.message ?: "Unknown failure obtaining servers!")
            }
        }
    }
}