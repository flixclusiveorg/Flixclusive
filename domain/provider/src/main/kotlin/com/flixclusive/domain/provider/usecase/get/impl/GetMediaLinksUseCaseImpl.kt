package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBWatchProvidersRepository
import com.flixclusive.domain.provider.R
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.util.extensions.getWatchId
import com.flixclusive.domain.provider.util.extensions.sendExtractingLinksMessage
import com.flixclusive.domain.provider.util.extensions.sendFetchingFilmMessage
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

internal class GetMediaLinksUseCaseImpl @Inject constructor(
    private val cachedLinksRepository: CachedLinksRepository,
    private val tmdbWatchProvidersRepository: TMDBWatchProvidersRepository,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
    private val userSessionDataStore: UserSessionDataStore,
) : GetMediaLinksUseCase {
    override operator fun invoke(
        movie: Movie,
        providerId: String?,
    ): Flow<LoadLinksState> =
        run(
            film = movie,
            providerId = providerId,
        )

    override operator fun invoke(
        tvShow: TvShow,
        episode: Episode,
        providerId: String?,
    ): Flow<LoadLinksState> =
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
    ): Flow<LoadLinksState> =
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
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val enabledProviders = providerRepository.getEnabledProviders(ownerId = userId)
        val oldCache = cachedLinksRepository.getCache(
            CacheKey.create(
                filmId = film.identifier,
                providerId = film.providerId,
                episode = episode,
            )
        )

        val isCached = oldCache?.isReady == true
        if (isCached && film.isFromTmdb && enabledProviders.isEmpty()) {
            send(LoadLinksState.SuccessWithTrustedProviders)
            return@channelFlow
        } else if (isCached) {
            send(LoadLinksState.Success(providerId = oldCache.providerId))
            return@channelFlow
        }

        if (enabledProviders.isEmpty() && film.isFromTmdb) {
            try {
                extractLinksFromTMDB(film = film, episode = episode)
            } catch (e: ExceptionWithUiText) {
                send(LoadLinksState.Error(e.uiText))
                return@channelFlow
            }

            send(LoadLinksState.SuccessWithTrustedProviders)
            return@channelFlow
        }

        /**
         * A nested function that processes each provider API to fetch media links.
         * */
        suspend fun processProviders(
            id: String,
            api: ProviderApi,
        ): Boolean {
            val metadata = providerRepository.getMetadata(id)
            if (metadata == null) {
                send(LoadLinksState.Error(UiText.from(R.string.provider_api_not_found, id)))
                return false
            }

            sendFetchingFilmMessage(provider = metadata.name)

            val cacheKey = CacheKey.create(
                filmId = film.identifier,
                providerId = id,
                episode = episode,
            )

            // Check if the cache already exists for this provider
            val cache = cachedLinksRepository.getCache(cacheKey)
                ?: CachedLinks(
                    providerId = id,
                    thumbnail = film.backdropImage ?: film.posterImage,
                )

            if (cache.isReady) {
                send(LoadLinksState.Success(providerId = cache.providerId))
                return true
            }

            if (watchId == null && !film.isFromTmdb) {
                send(
                    LoadLinksState.Error(
                        UiText.from(R.string.invalid_watch_id_for_non_tmdb_film),
                    ),
                )
                return false
            }

            val watchIdTouse = if (
                watchId == null
                && film.isFromTmdb
                && cache.watchId.isEmpty()
            ) {
                // Get the watch ID of the TMDB movie from the given provider API
                val response = api.getWatchId(film = film)
                if (response is Resource.Failure || response.data == null) {
                    val error = response.error ?: UiText.from(R.string.no_watch_id_message)
                    send(LoadLinksState.Error(error))
                    return false
                }

                response.data!!
            } else {
                watchId ?: cache.watchId.takeIf { it.isNotEmpty() }
            }

            if (watchIdTouse == null) {
                send(LoadLinksState.Error(UiText.from(R.string.no_watch_id_message)))
                return false
            }

            cachedLinksRepository.storeCache(
                key = cacheKey,
                cachedLinks = cache.copy(watchId = watchIdTouse),
            )

            sendExtractingLinksMessage(
                provider = metadata,
                isOnWebView = api is ProviderWebViewApi,
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
                    val cache = cachedLinksRepository.getCache(cacheKey)
                    if (cache != null && cache.hasStreamableLinks) {
                        cachedLinksRepository.storeCache(cacheKey, cache.copy(hasExtractedSuccessfully = true))
                        send(LoadLinksState.Success(providerId = cache.providerId))
                        return true
                    } else {
                        send(
                            LoadLinksState.Error(
                                UiText.from(R.string.no_links_loaded_format_message, metadata.name),
                            ),
                        )
                        return false
                    }
                }

                is Resource.Failure -> {
                    send(LoadLinksState.Error(result.error))
                    return false
                }

                Resource.Loading -> return false
            }
        }

        if (!film.isFromTmdb || providerId != null) {
            val id = providerId ?: film.providerId
            val api = providerRepository.getApi(id, userId)

            if (api == null) {
                send(LoadLinksState.Unavailable(UiText.from(R.string.provider_api_not_found, id)))
                return@channelFlow
            }

            processProviders(id = id, api = api)
        } else {
            enabledProviders.forEach { provider ->
                val id = provider.id

                val api = try {
                    providerRepository.getApi(id, userId)!!
                } catch (e: Throwable) {
                    errorLog("Failed to get API for provider with id: $id")
                    errorLog(e)
                    send(
                        LoadLinksState.Unavailable(
                            UiText.from(e.cause?.message ?: "UNKNOWN_PROVIDER_API_ERROR")
                        )
                    )
                    return@forEach
                }

                val success = processProviders(id = id, api = api)
                if (success) {
                    return@channelFlow
                }
            }
        }
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

                // Safe to call since this assumes that the user doesn't have any providers
                cachedLinksRepository.setCurrentCache(tmdbKey)
            }

            is Resource.Failure -> throw ExceptionWithUiText(response.error)
            Resource.Loading -> Unit
        }
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
}
