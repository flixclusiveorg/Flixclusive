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
        film: Film,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?> {
        return withContext(ioDispatcher) {
            try {
                providerApi.getSourceLinks(
                    filmId = mediaId,
                    film = film,
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
    ): Resource<String?> {
        return withContext(ioDispatcher) {
            try {
                if (film == null)
                    return@withContext Resource.Failure(UtilR.string.default_error)

                var i = 1
                var id: String? = null
                val maxPage = 3

                while (id == null) {
                    if (i > maxPage) {
                        return@withContext Resource.Success(null)
                    }

                    val searchResponse = providerApi.search(
                        page = i,
                        film = film
                    )

                    if (searchResponse.results.isEmpty())
                        return@withContext Resource.Success(null)


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
                        break
                    }
                }

                Resource.Success(id)
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                Resource.Failure(UtilR.string.source_data_dialog_state_unavailable_default)
            }
        }
    }
}