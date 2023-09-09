package com.flixclusive.data.repository

import com.flixclusive.R
import com.flixclusive.common.LoggerUtils.errorLog
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive_provider.interfaces.FilmSourcesProvider
import com.flixclusive_provider.models.common.MediaType
import com.flixclusive_provider.models.common.VideoData
import com.flixclusive_provider.utils.DecryptUtils.DecryptionException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class FilmSourcesRepositoryImpl @Inject constructor(
    private val provider: FilmSourcesProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FilmSourcesRepository {

    override suspend fun getStreamingLinks(
        mediaId: String,
        episodeId: String,
        server: String,
    ): Resource<VideoData?> {
        return withContext(ioDispatcher) {
            try {
                val availableServers = provider.getAvailableServers(
                    episodeId = episodeId,
                    mediaId = mediaId
                )
                var index = availableServers.indexOfFirst { it.serverName == server }

                for (retries in availableServers.indices) {
                    try {
                        if (index >= availableServers.size || index == -1) {
                            // Reset the index if it exceeds the available servers list
                            index = 0
                        }

                        val response = provider.getStreamingLinks(
                            episodeId = episodeId,
                            mediaId = mediaId,
                            server = availableServers[index]
                        )

                        return@withContext Resource.Success(
                            response.copy(servers = availableServers)
                        )
                    } catch (e: HttpException) {
                        index++
                    }
                }

                Resource.Failure("No servers are available!")
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                val errorMessageId = when (e) {
                    is SocketTimeoutException -> R.string.connection_timeout
                    is UnknownHostException, is HttpException -> R.string.connection_failed
                    is DecryptionException -> R.string.decryption_error
                    is JsonSyntaxException -> R.string.failed_to_extract
                    else -> R.string.video_data_dialog_state_error_default
                }

                Resource.Failure(errorMessageId)
            }
        }
    }

    override suspend fun getMediaId(film: Film?): String? {
        return withContext(ioDispatcher) {
            try {
                if (film == null)
                    return@withContext null

                var i = 1
                var id: String? = null
                val maxPage = 3

                while (id == null) {
                    if (i > maxPage) {
                        return@withContext ""
                    }

                    val searchResponse = provider.search(
                        query = film.title,
                        page = i
                    )

                    if (searchResponse.results.isEmpty())
                        return@withContext ""



                    searchResponse.results.forEach { result ->
                        val titleMatches = result.title.equals(film.title, ignoreCase = true)
                        val filmTypeMatches = result.mediaType?.type == film.filmType.type
                        val releaseDateMatches =
                            result.releaseDate == film.dateReleased.split(" ").last()

                        if (titleMatches && filmTypeMatches && film is TvShow) {
                            if (film.seasons.size == result.seasons) {
                                id = result.id
                                return@forEach
                            }

                            val tvShowInfo = provider.getMediaInfo(
                                mediaId = result.id,
                                mediaType = MediaType.TvShow
                            )
                            val tvReleaseDateMatches = tvShowInfo.releaseDate.split("-")
                                .first() == film.dateReleased.split("-").first()

                            if (tvReleaseDateMatches) {
                                id = result.id
                                return@forEach
                            }
                        } else if (titleMatches && filmTypeMatches && releaseDateMatches) {
                            id = result.id
                            return@forEach
                        }
                    }

                    if (searchResponse.hasNextPage) {
                        i++
                    } else if (id.isNullOrEmpty()) {
                        id = ""
                        break
                    }
                }

                id
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                null
            }
        }
    }

    override suspend fun getEpisodeId(
        mediaId: String,
        filmType: FilmType,
        episode: Int?,
        season: Int?,
    ): String? {
        return withContext(ioDispatcher) {
            when (filmType) {
                FilmType.MOVIE -> mediaId.split("-").last()
                FilmType.TV_SHOW -> {
                    try {
                        provider.getEpisodeId(
                            mediaId = mediaId,
                            episode = episode!!,
                            season = season!!,
                        )
                    } catch (e: Exception) {
                        errorLog(e.stackTraceToString())
                        null
                    }
                }
            }
        }
    }
}