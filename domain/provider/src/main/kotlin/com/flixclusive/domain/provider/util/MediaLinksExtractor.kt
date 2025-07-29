package com.flixclusive.domain.provider.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinks
import com.flixclusive.data.provider.cache.CachedLinksRepository
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
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.first
import com.flixclusive.core.locale.R as LocaleR

internal class MediaLinksExtractor(
    private val providerRepository: ProviderRepository,
    private val cachedLinksRepository: CachedLinksRepository,
    private val cachedLinksHelper: CachedLinksHelper,
) {
    suspend fun extract(
        scope: ProducerScope<MediaLinkResourceState>,
        film: FilmMetadata,
        episode: Episode?,
        providerId: String,
        api: ProviderApi,
        watchId: String?,
        canStopLooping: Boolean,
        onError: ((UiText) -> Unit)?,
    ): LinkExtractionResult {
        val metadata = providerRepository.getProviderMetadata(providerId)
            ?: return LinkExtractionResult.Continue

        scope.sendFetchingFilmMessage(provider = metadata.name)

        if (cachedLinksHelper.refreshProviderCache(film, episode, providerId)) {
            return LinkExtractionResult.Success
        }

        val watchIdResource = getCorrectWatchId(watchId, film, api)
        if (watchIdResource.data.isNullOrBlank()) {
            return if (canStopLooping) {
                val error = watchIdResource.error
                    ?: UiText.StringResource(LocaleR.string.blank_media_id_error_message)
                onError?.invoke(error)
                LinkExtractionResult.Error(error)
            } else {
                LinkExtractionResult.Continue
            }
        }

        val watchIdToUse = watchIdResource.data!!
        scope.sendExtractingLinksMessage(
            provider = metadata.name,
            isOnWebView = api is ProviderWebViewApi,
        )

        val providerCacheKey = CacheKey.create(
            filmId = film.identifier,
            providerId = providerId,
            episode = episode,
        )

        val defaultNewCache = CachedLinks(
            watchId = watchIdToUse,
            providerId = providerId,
            thumbnail = film.backdropImage ?: film.posterImage,
        )

        val providerCache = cachedLinksRepository.observeCache(providerCacheKey, defaultNewCache)

        val result = if (providerCache.first() == null) {
            getLinks(
                film = film,
                watchId = watchIdToUse,
                episode = episode,
                api = api,
                onLinkFound = { link ->
                    when (link) {
                        is Stream -> cachedLinksRepository.addStream(providerCacheKey, link)
                        is Subtitle -> cachedLinksRepository.addSubtitle(providerCacheKey, link)
                    }
                },
            )
        } else {
            Resource.Success(Unit)
        }

        return handleExtractionResult(
            result = result,
            cache = providerCache.first(),
            providerName = metadata.name,
            canStopLooping = canStopLooping,
            onError = onError,
        )
    }

    private suspend fun getCorrectWatchId(
        watchId: String?,
        film: FilmMetadata,
        api: ProviderApi,
    ): Resource<String?> {
        val needsNewWatchId = watchId == null && film.isFromTmdb
        return when {
            needsNewWatchId -> api.getWatchId(film = film)
            else -> Resource.Success(watchId ?: film.identifier)
        }
    }

    private fun handleExtractionResult(
        result: Resource<Unit>,
        cache: CachedLinks?,
        providerName: String,
        canStopLooping: Boolean,
        onError: ((UiText) -> Unit)?,
    ): LinkExtractionResult {
        val noLinksLoaded = result is Resource.Success && cache?.hasNoStreamLinks == true
        val isNotSuccessful = result is Resource.Failure || noLinksLoaded

        return when {
            isNotSuccessful && canStopLooping -> {
                val error = when {
                    result.error != null -> result.error!!
                    noLinksLoaded -> getNoLinksLoadedMessage(providerName)
                    else -> DEFAULT_ERROR_MESSAGE
                }
                onError?.invoke(error)
                LinkExtractionResult.Error(error)
            }

            isNotSuccessful -> LinkExtractionResult.Continue
            result is Resource.Success -> LinkExtractionResult.Success
            else -> LinkExtractionResult.Continue
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
    private suspend fun getLinks(
        api: ProviderApi,
        watchId: String,
        film: FilmMetadata,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit,
    ): Resource<Unit> {
        return withIOContext {
            var webView: ProviderWebView? = null

            try {
                if (api is ProviderWebViewApi) {
                    withMainContext {
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
                withMainContext {
                    webView?.destroy()
                }
            }
        }
    }

    sealed class LinkExtractionResult {
        object Success : LinkExtractionResult()

        object Continue : LinkExtractionResult()

        data class Error(
            val error: UiText,
        ) : LinkExtractionResult()
    }
}
