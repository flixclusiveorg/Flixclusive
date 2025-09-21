package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBWatchProvidersRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.util.extensions.getWatchId
import com.flixclusive.domain.provider.util.extensions.sendExtractingLinksMessage
import com.flixclusive.domain.provider.util.extensions.sendFetchingFilmMessage
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.ProviderWebViewApi
import com.flixclusive.provider.webview.ProviderWebView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

internal class GetMediaLinksUseCaseImpl
    @Inject
    constructor(
        private val cachedLinksRepository: CachedLinksRepository,
        private val tmdbWatchProvidersRepository: TMDBWatchProvidersRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val providerRepository: ProviderRepository,
        private val appDispatchers: AppDispatchers,
    ) : GetMediaLinksUseCase {
        /**
         * Returns a list of [ProviderApiWithId] that contains the ID and API for each provider.
         *
         * This list is used to fetch media links for the given [Movie], [TvShow], or [Film].
         * */
        private val apis: List<ProviderApiWithId>
            get() {
                var providers = providerRepository.getOrderedProviders()

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

        override operator fun invoke(
            movie: Movie,
            providerId: String?,
        ): Flow<MediaLinkResourceState> =
            run(
                film = movie,
                providerId = providerId,
            )

        override operator fun invoke(
            tvShow: TvShow,
            episode: Episode,
            providerId: String?,
        ): Flow<MediaLinkResourceState> =
            run(
                film = tvShow,
                episode = episode,
                providerId = providerId,
            )

        override operator fun invoke(
            film: FilmMetadata,
            watchId: String,
            episode: Episode?,
            providerId: String?,
        ): Flow<MediaLinkResourceState> =
            run(
                film = film,
                episode = episode,
                watchId = watchId,
                providerId = providerId,
            )

        private fun run(
            film: FilmMetadata,
            watchId: String? = null,
            episode: Episode? = null,
            providerId: String? = null,
        ) = channelFlow {
            val isCached = film.isCached(providerId, episode)

            if (isCached && film.isFromTmdb && apis.isEmpty()) {
                send(MediaLinkResourceState.SuccessWithTrustedProviders)
                return@channelFlow
            } else if (isCached) {
                send(MediaLinkResourceState.Success)
                return@channelFlow
            }

            if (apis.isEmpty() && film.isFromTmdb) {
                try {
                    extractLinksFromTMDB(film = film, episode = episode)
                } catch (e: ExceptionWithUiText) {
                    send(MediaLinkResourceState.Error(e.uiText))
                    return@channelFlow
                }

                send(MediaLinkResourceState.SuccessWithTrustedProviders)
                return@channelFlow
            }

            /**
             * A nested function that processes each provider API to fetch media links.
             * */
            suspend fun processProviders(
                id: String,
                api: ProviderApi,
            ): Boolean {
                val metadata = providerRepository.getProviderMetadata(id)
                if (metadata == null) {
                    send(MediaLinkResourceState.Error(UiText.from(R.string.provider_api_not_found, id)))
                    return false
                }

                sendFetchingFilmMessage(provider = metadata.name)

                val cacheKey = CacheKey.create(
                    filmId = film.identifier,
                    providerId = id,
                    episode = episode,
                )

                // Check if the cache already exists for this provider
                val existingCache = cachedLinksRepository.getCache(cacheKey)
                if (existingCache?.hasStreamableLinks == true) {
                    cachedLinksRepository.reuseCache(cacheKey, existingCache)
                    send(MediaLinkResourceState.Success)
                    return true
                }

                if (watchId == null && !film.isFromTmdb) {
                    send(
                        MediaLinkResourceState.Error(
                            UiText.from(R.string.invalid_watch_id_for_non_tmdb_film),
                        ),
                    )
                    return false
                }

                val watchIdTouse = if (watchId == null && film.isFromTmdb) {
                    // Get the watch ID of the TMDB movie from the given provider API
                    val response = api.getWatchId(film = film)
                    if (response is Resource.Failure || response.data == null) {
                        val error = response.error ?: UiText.from(R.string.no_watch_id_message)
                        send(MediaLinkResourceState.Error(error))
                        return false
                    }

                    response.data!!
                } else {
                    watchId!!
                }

                sendExtractingLinksMessage(
                    provider = metadata.name,
                    isOnWebView = api is ProviderWebViewApi,
                )

                cachedLinksRepository.storeCache(
                    key = cacheKey,
                    cachedLinks = CachedLinks(
                        watchId = watchIdTouse,
                        providerId = id,
                        thumbnail = film.backdropImage ?: film.posterImage,
                        episode = episode,
                    ),
                )

                val result = getMediaLinks(
                    film = film,
                    episode = episode,
                    watchId = watchIdTouse,
                    api = api,
                    onLinkFound = { link ->
                        when (link) {
                            is Stream -> cachedLinksRepository.addStream(cacheKey, link)
                            is Subtitle -> cachedLinksRepository.addSubtitle(cacheKey, link)
                        }
                    },
                )

                when (result) {
                    is Resource.Success -> {
                        // Get the current cache after the links have been extracted
                        val cache = cachedLinksRepository.currentCache.first()

                        if (cache?.hasNoStreamLinks == true) {
                            send(
                                MediaLinkResourceState.Error(
                                    UiText.from(R.string.no_links_loaded_format_message, metadata.name),
                                ),
                            )
                            return false
                        } else {
                            send(MediaLinkResourceState.Success)
                            return true
                        }
                    }

                    is Resource.Failure -> {
                        send(MediaLinkResourceState.Error(result.error))
                        return false
                    }

                    Resource.Loading -> return false
                }
            }

            if (!film.isFromTmdb || providerId != null) {
                val id = providerId ?: film.providerId
                val api = providerApiRepository.getApi(id)

                if (api == null) {
                    send(MediaLinkResourceState.Unavailable(UiText.from(R.string.provider_api_not_found, id)))
                    return@channelFlow
                }

                processProviders(id = id, api = api)
            } else {
                apis.forEach { (id, api) ->
                    val success = processProviders(id = id, api = api)

                    if (success) {
                        return@channelFlow
                    }
                }
            }
        }

        private fun FilmMetadata.isCached(
            providerId: String?,
            episode: Episode? = null,
        ): Boolean {
            val cacheKey = if (providerId != null) {
                CacheKey.create(
                    filmId = identifier,
                    providerId = providerId,
                    episode = episode,
                )
            } else if (isFromTmdb && apis.isEmpty()) {
                CacheKey.create(
                    filmId = identifier,
                    providerId = DEFAULT_FILM_SOURCE_NAME,
                    episode = episode,
                )
            } else {
                return false
            }

            val cache = cachedLinksRepository.getCache(cacheKey)
                ?: return false

            return cache.hasStreamableLinks
        }

        /**
         * Extracts links from TMDB watch providers for a given [FilmMetadata].
         * */
        @Throws(ExceptionWithUiText::class)
        private suspend fun extractLinksFromTMDB(
            film: FilmMetadata,
            episode: Episode? = null,
        ) {
            when (
                val response = tmdbWatchProvidersRepository.getWatchProviders(
                    mediaType = film.filmType.type,
                    id = film.tmdbId!!,
                )
            ) {
                is Resource.Success<*> -> {
                    val streams = response.data

                    if (streams.isNullOrEmpty()) {
                        throw ExceptionWithUiText(UiText.from(LocaleR.string.no_available_providers))
                    }

                    val tmdbKey = CacheKey.create(
                        filmId = film.identifier,
                        providerId = DEFAULT_FILM_SOURCE_NAME,
                        episode = episode,
                    )

                    val cache = CachedLinks(
                        watchId = film.identifier,
                        providerId = DEFAULT_FILM_SOURCE_NAME,
                        thumbnail = film.backdropImage ?: film.posterImage,
                        streams = streams,
                    )

                    cachedLinksRepository.storeCache(tmdbKey, cache)
                }

                is Resource.Failure -> throw ExceptionWithUiText(response.error)
                Resource.Loading -> Unit
            }
        }

        /**
         * Refreshes the cache for a given [CacheKey] with the provided [CachedLinks].
         *
         * This function removes the existing cache for the given key and stores the new cache.
         *
         * The reason for removing first is to ensure th
         * */
        private fun CachedLinksRepository.reuseCache(
            cacheKey: CacheKey,
            cache: CachedLinks,
        ) {
            removeCache(cacheKey)
            storeCache(cacheKey, cache)
        }

        /**
         *
         * Obtains the list of [MediaLink] of a given [FilmMetadata] from the given [ProviderApi].
         *
         * @param api The api to be used to obtain the links.
         * @param watchId The unique identifier to be used to obtain the links.
         * @param film A detailed film object used to obtain the links. It could either be a [Movie] or a [TvShow]
         * @param episode An episode data used to obtain the links if the [film] parameter is a [TvShow]
         * @param onLinkFound A callback function that is invoked when a [Stream] or [Subtitle] is found.
         *
         * @return a [Resource] of [List] of [MediaLink]
         * */
        private suspend fun getMediaLinks(
            api: ProviderApi,
            watchId: String,
            film: FilmMetadata,
            episode: Episode? = null,
            onLinkFound: (MediaLink) -> Unit,
        ): Resource<Unit> {
            return withContext(appDispatchers.io) {
                var webView: ProviderWebView? = null

                try {
                    if (api is ProviderWebViewApi) {
                        withContext(appDispatchers.main) {
                            webView = api.getWebView()
                        }

                        webView!!.getLinks(
                            watchId = watchId,
                            film = film,
                            episode = episode,
                            onLinkFound = onLinkFound,
                        )
                    } else {
                        api.getLinks(
                            watchId = watchId,
                            film = film,
                            episode = episode,
                            onLinkFound = onLinkFound,
                        )
                    }

                    Resource.Success(Unit)
                } catch (e: Throwable) {
                    e.toNetworkException()
                } finally {
                    withContext(appDispatchers.main) {
                        webView?.destroy()
                    }
                }
            }
        }

        private data class ProviderApiWithId(
            val id: String,
            val api: ProviderApi,
        )
    }
