package com.flixclusive.domain.provider.fetch

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBRepository
import com.flixclusive.domain.provider.util.TmdbHelper
import com.flixclusive.domain.provider.util.cache.CachedLinksHelper
import com.flixclusive.domain.provider.util.cache.EMPTY_PROVIDER_MESSAGE
import com.flixclusive.domain.provider.util.cache.MediaLinksExtractor
import com.flixclusive.domain.provider.util.cache.UNAVAILABLE_EPISODE_MESSAGE
import com.flixclusive.domain.provider.util.extensions.finish
import com.flixclusive.domain.provider.util.extensions.finishWithTrustedProviders
import com.flixclusive.domain.provider.util.extensions.sendFetchingEpisodeMessage
import com.flixclusive.domain.provider.util.extensions.throwError
import com.flixclusive.domain.provider.util.extensions.throwUnavailableError
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetMediaLinksUseCase
    @Inject
    constructor(
        cachedLinksRepository: CachedLinksRepository,
        tmdbRepository: TMDBRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val providerRepository: ProviderRepository,
    ) {
        private val cachedLinksHelper = CachedLinksHelper(cachedLinksRepository)
        private val mediaLinksExtractor = MediaLinksExtractor(
            cachedLinksRepository = cachedLinksRepository,
            providerRepository = providerRepository,
            cachedLinksHelper = cachedLinksHelper,
        )
        private val tmdbHelper = TmdbHelper(
            tmdbRepository = tmdbRepository,
            cachedLinksHelper = cachedLinksHelper,
        )

        /**
         *
         * Obtains the links of the film and episode.
         *
         * @param film The film to get the links
         * @param watchHistory The watch history item of the film
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
            watchHistory: WatchHistory?,
            preferredProvider: String? = null,
            watchId: String? = null,
            episode: Episode? = null,
            onSuccess: (Episode?) -> Unit,
            onError: ((UiText) -> Unit)? = null, // TODO: Remove this and use MediaLinkResourceState.Error type instead
        ): Flow<MediaLinkResourceState> =
            channelFlow {
                val resolvedEpisode = resolveEpisodeIfNeeded(
                    scope = this,
                    film = film,
                    watchHistory = watchHistory,
                    episode = episode,
                    onError = onError,
                )

                if (resolvedEpisode == null && film.isTvShow) {
                    return@channelFlow
                }

                val apis = getPrioritizedProvidersList(preferredProvider)

                val hasCachedLinks = containsCachedLinks(
                    scope = this,
                    film = film,
                    episode = resolvedEpisode,
                    preferredProvider = preferredProvider,
                    apis = apis,
                    onSuccess = onSuccess,
                )

                if (hasCachedLinks) {
                    return@channelFlow
                }

                if (apis.isEmpty()) {
                    val success = extractFromTmdb(
                        scope = this,
                        film = film,
                        episode = resolvedEpisode,
                        onError = onError,
                    )

                    if (success) {
                        return@channelFlow
                    }

                    onError?.invoke(EMPTY_PROVIDER_MESSAGE)
                    throwUnavailableError(EMPTY_PROVIDER_MESSAGE)
                    return@channelFlow
                }

                processProviders(
                    scope = this,
                    film = film,
                    episode = resolvedEpisode,
                    apis = apis,
                    preferredProvider = preferredProvider,
                    watchId = watchId,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }

        private suspend fun resolveEpisodeIfNeeded(
            scope: ProducerScope<MediaLinkResourceState>,
            film: FilmMetadata,
            watchHistory: WatchHistory?,
            episode: Episode?,
            onError: ((UiText) -> Unit)?,
        ): Episode? {
            if (!film.isTvShow || episode != null) {
                return episode
            }

            scope.sendFetchingEpisodeMessage()
            val nextEpisode = tmdbHelper.getNearestEpisodeToWatch(film as TvShow, watchHistory)

            if (nextEpisode is Resource.Failure) {
                onError?.invoke(nextEpisode.error ?: UNAVAILABLE_EPISODE_MESSAGE)
                scope.throwError(nextEpisode.error)
                return null
            }

            return nextEpisode.data
        }

        private fun containsCachedLinks(
            scope: ProducerScope<MediaLinkResourceState>,
            film: FilmMetadata,
            episode: Episode?,
            preferredProvider: String?,
            apis: List<ProviderApiWithId>,
            onSuccess: (Episode?) -> Unit,
        ): Boolean {
            val previousCache = cachedLinksHelper.getCacheForFilm(film, episode, preferredProvider, apis.isNotEmpty())

            if (cachedLinksHelper.isCacheValid(previousCache, preferredProvider)) {
                onSuccess(episode)
                if (film.isFromTmdb && apis.isEmpty()) {
                    scope.finishWithTrustedProviders()
                } else {
                    scope.finish()
                }
                return true
            }
            return false
        }

        private suspend fun extractFromTmdb(
            scope: ProducerScope<MediaLinkResourceState>,
            film: FilmMetadata,
            episode: Episode?,
            onError: ((UiText) -> Unit)?,
        ): Boolean {
            if (!film.isFromTmdb) return false

            val result = tmdbHelper.extractProviders(film, episode)
            if (result is Resource.Success) {
                scope.finishWithTrustedProviders()
                return true
            } else {
                onError?.invoke(result.error!!)
                scope.throwError(result.error)
                return true
            }
        }

        private suspend fun processProviders(
            scope: ProducerScope<MediaLinkResourceState>,
            film: FilmMetadata,
            episode: Episode?,
            apis: List<ProviderApiWithId>,
            preferredProvider: String?,
            watchId: String?,
            onSuccess: (Episode?) -> Unit,
            onError: ((UiText) -> Unit)?,
        ) {
            for (i in apis.indices) {
                val (apiId, api) = apis[i]

                if (!shouldProcessProvider(film, apiId, preferredProvider)) {
                    continue
                }

                val canStopLooping = i == apis.lastIndex || preferredProvider != null || !film.isFromTmdb

                when (
                    val result = mediaLinksExtractor.extract(
                        scope = scope,
                        film = film,
                        episode = episode,
                        providerId = apiId,
                        api = api,
                        watchId = watchId,
                        canStopLooping = canStopLooping,
                        onError = onError,
                    )
                ) {
                    MediaLinksExtractor.LinkExtractionResult.Success -> {
                        onSuccess(episode)
                        scope.trySend(MediaLinkResourceState.Success)
                        return
                    }

                    is MediaLinksExtractor.LinkExtractionResult.Error -> {
                        scope.trySend(MediaLinkResourceState.Error(result.error))
                        return
                    }

                    MediaLinksExtractor.LinkExtractionResult.Continue -> {
                        continue
                    }
                }
            }

            scope.trySend(MediaLinkResourceState.Unavailable())
        }

        private fun shouldProcessProvider(
            film: FilmMetadata,
            apiId: String,
            preferredProvider: String?,
        ): Boolean {
            val isNotTheSameFilmProvider = !film.providerId.equals(apiId, true) && !film.isFromTmdb
            val isChangingProvider = preferredProvider != null

            return !((isChangingProvider && apiId != preferredProvider) || isNotTheSameFilmProvider)
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
    }

private data class ProviderApiWithId(
    val id: String,
    val api: ProviderApi,
)
