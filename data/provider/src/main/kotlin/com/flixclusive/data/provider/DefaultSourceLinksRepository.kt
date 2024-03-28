package com.flixclusive.data.provider

import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.network.CryptographyUtil
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.ProviderApi
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.flixclusive.core.util.R as UtilR

class DefaultSourceLinksRepository @Inject constructor(
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SourceLinksRepository {

    override suspend fun getSourceLinks(
        mediaId: String,
        providerApi: ProviderApi,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?> {
        return withContext(ioDispatcher) {
            try {
                providerApi.getSourceLinks(
                    filmId = mediaId,
                    episode = episode,
                    season = season,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )

                return@withContext Resource.Success(Unit)
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                return@withContext when (e) {
                    is CryptographyUtil.DecryptionException -> Resource.Failure(UtilR.string.decryption_error)
                    is JsonSyntaxException -> Resource.Failure(UtilR.string.failed_to_extract)
                    else -> e.catchInternetRelatedException()
                }
            }
        }
    }

    override suspend fun getMediaId(
        film: Film?,
        providerApi: ProviderApi,
    ): String? {
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

                    val searchResponse = providerApi.search(
                        query = film.title,
                        page = i,
                        filmType = film.filmType
                    )

                    if (searchResponse.results.isEmpty())
                        return@withContext ""


                    for(result in searchResponse.results) {
                        if(result.tmdbId == film.id) {
                            id = result.id
                            break
                        }

                        val titleMatches = result.title.equals(film.title, ignoreCase = true)
                        val filmTypeMatches = result.filmType?.type == film.filmType.type
                        val releaseDateMatches =
                            result.releaseDate == film.dateReleased.split(" ").last()

                        if (titleMatches && filmTypeMatches && film is TvShow) {
                            if (film.seasons.size == result.seasons || releaseDateMatches) {
                                id = result.id
                                break
                            }

                            val tvShowInfo = providerApi.getFilmInfo(
                                filmId = result.id!!,
                                filmType = film.filmType
                            )

                            val tvReleaseDateMatches = tvShowInfo.yearReleased == film.dateReleased.split("-").first()
                            val seasonCountMatches = film.seasons.size == tvShowInfo.seasons

                            if (tvReleaseDateMatches || seasonCountMatches) {
                                id = result.id
                                break
                            }
                        }

                        if (titleMatches && filmTypeMatches && releaseDateMatches) {
                            id = result.id
                            break
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
}