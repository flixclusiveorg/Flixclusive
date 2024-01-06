package com.flixclusive.data.provider

import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.core.util.common.network.AppDispatchers
import com.flixclusive.core.util.common.network.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.exception.catchInternetRelatedException
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.provider.BaseProvider
import com.flixclusive.provider.utils.DecryptUtils
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultSourceLinksRepository @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SourceLinksRepository {

    override suspend fun getSourceLinks(
        mediaId: String,
        provider: BaseProvider,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?> {
        return withContext(ioDispatcher) {
            try {
                provider.getSourceLinks(
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
                    is DecryptUtils.DecryptionException -> Resource.Failure(R.string.decryption_error)
                    is JsonSyntaxException -> Resource.Failure(R.string.failed_to_extract)
                    else -> e.catchInternetRelatedException()
                }
            }
        }
    }

    override suspend fun getMediaId(
        film: Film?,
        provider: BaseProvider,
    ): String? {
        return withContext(ioDispatcher) {
            try {
                val filmToSearch = if(film?.filmType == FilmType.TV_SHOW && film !is TvShow) {
                    val response = tmdbRepository.getTvShow(film.id)

                    if(response is Resource.Failure)
                        return@withContext null

                    response.data
                } else film

                if (filmToSearch == null)
                    return@withContext null

                var i = 1
                var id: String? = null
                val maxPage = 3

                while (id == null) {
                    if (i > maxPage) {
                        return@withContext ""
                    }

                    val searchResponse = provider.search(
                        query = filmToSearch.title,
                        page = i,
                        filmType = filmToSearch.filmType
                    )

                    if (searchResponse.results.isEmpty())
                        return@withContext ""


                    for(result in searchResponse.results) {
                        if(result.tmdbId == filmToSearch.id) {
                            id = result.id
                            break
                        }

                        val titleMatches = result.title.equals(filmToSearch.title, ignoreCase = true)
                        val filmTypeMatches = result.mediaType?.type == filmToSearch.filmType.type
                        val releaseDateMatches =
                            result.releaseDate == filmToSearch.dateReleased.split(" ").last()

                        if (titleMatches && filmTypeMatches && filmToSearch is TvShow) {
                            if (filmToSearch.seasons.size == result.seasons || releaseDateMatches) {
                                id = result.id
                                break
                            }

                            val tvShowInfo = provider.getFilmInfo(
                                filmId = result.id!!,
                                filmType = filmToSearch.filmType
                            )

                            val tvReleaseDateMatches = tvShowInfo.yearReleased == filmToSearch.dateReleased.split("-").first()
                            val seasonCountMatches = filmToSearch.seasons.size == tvShowInfo.seasons

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