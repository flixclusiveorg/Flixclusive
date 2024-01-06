package com.flixclusive.data.repository

import com.flixclusive.R
import com.flixclusive.data.utils.catchInternetRelatedException
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.FilmType.Companion.toMediaType
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.SourceLinksRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.providers.sources.SourceProvider
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.utils.DecryptUtils.DecryptionException
import com.flixclusive.utils.LoggerUtils.errorLog
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SourceLinksRepositoryImpl @Inject constructor(
    private val providersRepository: ProvidersRepository,
    private val tmdbRepository: TMDBRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SourceLinksRepository {

    override suspend fun getSourceLinks(
        mediaId: String,
        provider: SourceProvider,
        season: Int?,
        episode: Int?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?> {
        return withContext(ioDispatcher) {
            try {
                provider.getSourceLinks(
                    mediaId = mediaId,
                    episode = episode,
                    season = season,
                    onLinkLoaded = onLinkLoaded,
                    onSubtitleLoaded = onSubtitleLoaded
                )

                return@withContext Resource.Success(Unit)
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                return@withContext when (e) {
                    is DecryptionException -> Resource.Failure(R.string.decryption_error)
                    is JsonSyntaxException -> Resource.Failure(R.string.failed_to_extract)
                    else -> e.catchInternetRelatedException()
                }
            }
        }
    }

    override suspend fun getMediaId(
        film: Film?,
        provider: SourceProvider,
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
                        mediaType = filmToSearch.filmType.toMediaType()
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

                            val tvShowInfo = provider.getMediaInfo(
                                mediaId = result.id!!,
                                mediaType = MediaType.TvShow
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