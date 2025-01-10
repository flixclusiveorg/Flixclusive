package com.flixclusive.data.provider

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLinksRepository
    @Inject
    constructor() {
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
        suspend fun getLinks(
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
    }
