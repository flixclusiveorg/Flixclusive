package com.flixclusive.domain.provider

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.data.provider.MediaLinksRepository
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinks
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.domain.provider.util.DEFAULT_ERROR_MESSAGE
import com.flixclusive.domain.provider.util.EMPTY_PROVIDER_MESSAGE
import com.flixclusive.domain.provider.util.UNAVAILABLE_EPISODE_MESSAGE
import com.flixclusive.domain.provider.util.finish
import com.flixclusive.domain.provider.util.finishWithTrustedProviders
import com.flixclusive.domain.provider.util.getNoLinksLoadedMessage
import com.flixclusive.domain.provider.util.getWatchId
import com.flixclusive.domain.provider.util.isCached
import com.flixclusive.domain.provider.util.sendExtractingLinksMessage
import com.flixclusive.domain.provider.util.sendFetchingEpisodeMessage
import com.flixclusive.domain.provider.util.sendFetchingFilmMessage
import com.flixclusive.domain.provider.util.throwError
import com.flixclusive.domain.provider.util.throwUnavailableError
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.ProviderWebViewApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

@Singleton
class GetMediaLinksUseCase
    @Inject
    constructor(
        private val tmdbRepository: TMDBRepository,
        private val cachedLinksRepository: CachedLinksRepository,
        private val mediaLinksRepository: MediaLinksRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val providerRepository: ProviderRepository,
    ) {
        /**
         *
         * Obtains the links of the film and episode.
         *
         * @param film The film to get the links
         * @param watchHistoryItem The watch history item of the film
         * @param preferredProvider The ID of the preferred provider
         * @param watchId The watch id of the film
         * @param episode The episode of the film if it is a tv show
         * @param onSuccess A callback to run when the links are obtained successfully
         * @param onError A callback to run when an error occurs
         *
         * @return A flow stream of [MediaLinkResourceState]
         * */
        operator fun invoke(
            film: FilmMetadata,
            watchHistoryItem: WatchHistoryItem?,
            preferredProvider: String? = null,
            watchId: String? = null,
            episode: Episode? = null,
            onSuccess: (Episode?) -> Unit,
            onError: ((UiText) -> Unit)? = null,
        ): Flow<MediaLinkResourceState> =
            channelFlow {
                val apis =
                    getPrioritizedProvidersList(
                        preferredProvider = preferredProvider,
                    )

                var episodeToUse: Episode? = episode
                if (film.isTvShow && episode == null) {
                    sendFetchingEpisodeMessage()

                    val nextEpisode =
                        getNearestEpisodeToWatch(
                            film = film as TvShow,
                            watchHistoryItem = watchHistoryItem,
                        )

                    if (nextEpisode is Resource.Failure) {
                        onError?.invoke(nextEpisode.error ?: UNAVAILABLE_EPISODE_MESSAGE)
                        throwError(nextEpisode.error)
                        return@channelFlow
                    }

                    episodeToUse = nextEpisode.data
                }

                val previousCache =
                    if (preferredProvider != null) {
                        val preferredKey =
                            CacheKey.create(
                                filmId = film.identifier,
                                providerId = preferredProvider,
                                episode = episodeToUse,
                            )

                        cachedLinksRepository.getCache(preferredKey)
                    } else if (apis.isEmpty() || film.isFromTmdb) {
                        val tmdbKey =
                            CacheKey.create(
                                filmId = film.identifier,
                                providerId = DEFAULT_FILM_SOURCE_NAME,
                                episode = episodeToUse,
                            )

                        cachedLinksRepository.getCache(tmdbKey)
                    } else {
                        null
                    }

                if (previousCache?.isCached(preferredProvider) == true) {
                    onSuccess(episodeToUse)

                    if (film.isFromTmdb && apis.isEmpty()) {
                        finishWithTrustedProviders()
                    } else {
                        finish()
                    }

                    return@channelFlow
                }

                if (apis.isEmpty()) {
                    if (film.isFromTmdb) {
                        val response = loadOfficialWatchProvidersFromTmdb(film = film)

                        if (response is Resource.Success) {
                            val tmdbKey =
                                CacheKey.create(
                                    filmId = film.identifier,
                                    providerId = DEFAULT_FILM_SOURCE_NAME,
                                    episode = episodeToUse,
                                )

                            val cache =
                                CachedLinks(
                                    watchId = film.identifier,
                                    providerId = DEFAULT_FILM_SOURCE_NAME,
                                    thumbnail = film.backdropImage ?: film.posterImage,
                                    streams = response.data ?: emptyList(),
                                )

                            cachedLinksRepository.storeCache(tmdbKey, cache)
                            finishWithTrustedProviders()
                            return@channelFlow
                        } else {
                            onError?.invoke(response.error!!)
                            throwError(response.error)
                            return@channelFlow
                        }
                    }

                    onError?.invoke(EMPTY_PROVIDER_MESSAGE)
                    throwUnavailableError(EMPTY_PROVIDER_MESSAGE)
                    return@channelFlow
                }

                for (i in apis.indices) {
                    val (apiId, api) = apis[i]
                    val metadata = providerRepository.getProviderMetadata(apiId) ?: continue

                    val isNotTheSameFilmProvider = !film.providerId.equals(apiId, true) && !film.isFromTmdb
                    val isChangingProvider = preferredProvider != null

                    if ((isChangingProvider && apiId != preferredProvider) || isNotTheSameFilmProvider) {
                        continue
                    }

                    val canStopLooping = i == apis.lastIndex || isChangingProvider || !film.isFromTmdb

                    sendFetchingFilmMessage(provider = metadata.name)

                    val providerCacheKey =
                        CacheKey.create(
                            filmId = film.identifier,
                            providerId = apiId,
                            episode = episodeToUse,
                        )

                    val oldProviderCache = cachedLinksRepository.getCache(providerCacheKey)
                    if (oldProviderCache != null) {
                        cachedLinksRepository.removeCache(providerCacheKey)
                        cachedLinksRepository.storeCache(providerCacheKey, oldProviderCache)
                        onSuccess(episodeToUse)
                        finish()
                        return@channelFlow
                    }

                    val watchIdResource =
                        getCorrectWatchId(
                            watchId = watchId,
                            film = film,
                            api = api,
                        )

                    if (watchIdResource.data.isNullOrBlank()) {
                        if (canStopLooping) {
                            val error =
                                watchIdResource.error
                                    ?: UiText.StringResource(LocaleR.string.blank_media_id_error_message)

                            onError?.invoke(error)
                            throwUnavailableError(error)
                            return@channelFlow
                        }

                        continue
                    }

                    val watchIdToUse = watchIdResource.data!!
                    sendExtractingLinksMessage(
                        provider = metadata.name,
                        isOnWebView = api is ProviderWebViewApi,
                    )

                    val defaultNewCache =
                        CachedLinks(
                            watchId = watchIdToUse,
                            providerId = apiId,
                            thumbnail = film.backdropImage ?: film.posterImage,
                        )

                    val providerCache = cachedLinksRepository.observeCache(providerCacheKey, defaultNewCache)

                    val result =
                        if (providerCache.first() == null) {
                            mediaLinksRepository.getLinks(
                                film = film,
                                watchId = watchIdToUse,
                                episode = episodeToUse,
                                api = api,
                                onLinkFound = {
                                    when (it) {
                                        is Stream -> cachedLinksRepository.addStream(providerCacheKey, it)
                                        is Subtitle -> cachedLinksRepository.addSubtitle(providerCacheKey, it)
                                    }
                                },
                            )
                        } else {
                            Resource.Success(Unit)
                        }

                    val noLinksLoaded = result is Resource.Success && providerCache.first()!!.hasNoStreamLinks == true
                    val isNotSuccessful = result is Resource.Failure || noLinksLoaded

                    when {
                        isNotSuccessful && canStopLooping -> {
                            val error =
                                when {
                                    result.error != null -> result.error!!
                                    noLinksLoaded -> getNoLinksLoadedMessage(metadata.name)
                                    else -> DEFAULT_ERROR_MESSAGE
                                }

                            onError?.invoke(error)
                            trySend(MediaLinkResourceState.Error(error))
                            return@channelFlow
                        }

                        isNotSuccessful -> {
                            continue
                        }

                        result is Resource.Success -> {
                            onSuccess(episodeToUse)
                            trySend(MediaLinkResourceState.Success)
                            return@channelFlow
                        }
                    }
                }

                trySend(MediaLinkResourceState.Unavailable())
            }

        /**
         *
         * Obtains the [Episode] data of the nearest
         * episode to be watched by the user
         *
         * */
        private suspend fun getNearestEpisodeToWatch(
            film: TvShow,
            watchHistoryItem: WatchHistoryItem?,
        ): Resource<Episode?> {
            val isNewlyWatchShow =
                watchHistoryItem == null || watchHistoryItem.episodesWatched.isEmpty()

            val seasonNumber: Int
            val episodeNumber: Int

            if (isNewlyWatchShow) {
                if (film.totalSeasons == 0 || film.totalEpisodes == 0) {
                    return Resource.Failure(MediaLinkResourceState.Unavailable().message)
                }

                seasonNumber = 1
                episodeNumber = 1
            } else {
                val (nextSeason, nextEpisode) = getNextEpisodeToWatch(watchHistoryItem!!)
                seasonNumber = nextSeason ?: 1
                episodeNumber = nextEpisode ?: 1
            }

            return if (film.isFromTmdb) {
                getEpisodeFromTmdb(
                    tmdbId = film.tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )
            } else {
                val episode =
                    film.seasons
                        .find {
                            it.number == seasonNumber
                        }?.episodes
                        ?.find {
                            it.number == episodeNumber
                        }

                if (episode == null) {
                    return Resource.Failure(LocaleR.string.unavailable_episode)
                }

                Resource.Success(episode)
            }
        }

        private suspend fun getEpisodeFromTmdb(
            tmdbId: Int?,
            seasonNumber: Int,
            episodeNumber: Int,
        ): Resource<Episode?> {
            return tmdbRepository
                .getEpisode(
                    id = tmdbId ?: return Resource.Failure(LocaleR.string.invalid_tmdb_id),
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                ).also {
                    if (it is Resource.Failure) {
                        return it
                    } else if (it is Resource.Success && it.data == null) {
                        return Resource.Failure(LocaleR.string.unavailable_episode)
                    }
                }
        }

        /**
         *
         * Returns a list of providers
         * available that prioritizes the
         * given provider and puts it on top of the list
         * */
        private fun getPrioritizedProvidersList(preferredProvider: String?): List<ProviderApiWithId> {
            var providers = providerRepository.getOrderedProviders()
            if (preferredProvider != null) {
                providers =
                    providers.sortedByDescending {
                        it.id.equals(preferredProvider, true)
                    }
            }

            return providers
                .mapNotNull { provider ->
                    providerApiRepository.getApi(provider.id)?.let { api ->
                        ProviderApiWithId(
                            id = provider.id,
                            api = api,
                        )
                    }
                }
        }

        private suspend fun getCorrectWatchId(
            watchId: String?,
            film: FilmMetadata,
            api: ProviderApi,
        ): Resource<String?> {
            val needsNewWatchId = watchId == null && film.isFromTmdb
            val watchIdResource =
                when {
                    needsNewWatchId -> api.getWatchId(film = film)
                    else -> Resource.Success(watchId ?: film.identifier)
                }

            return watchIdResource
        }

        private suspend fun loadOfficialWatchProvidersFromTmdb(film: FilmMetadata): Resource<List<Stream>> {
            val response =
                tmdbRepository.getWatchProviders(
                    mediaType = film.filmType.type,
                    id = film.tmdbId!!,
                )

            return if (response is Resource.Failure) {
                Resource.Failure(response.error)
            } else {
                Resource.Success(response.data ?: emptyList())
            }
        }
    }

private data class ProviderApiWithId(
    val id: String,
    val api: ProviderApi,
)
