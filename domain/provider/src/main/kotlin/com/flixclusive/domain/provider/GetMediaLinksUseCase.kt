package com.flixclusive.domain.provider

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.flixclusive.core.util.common.dispatcher.AppDispatchers
import com.flixclusive.core.util.common.dispatcher.Dispatcher
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.provider.MediaLinksRepository
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.finish
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.sendExtractingLinksMessage
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.sendFetchingEpisodeMessage
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.sendFetchingFilmMessage
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.throwError
import com.flixclusive.domain.provider.util.GetMediaLinksStateMessageHelper.throwUnavailableError
import com.flixclusive.domain.provider.util.MediaLinksProviderUtil.DEFAULT_ERROR_MESSAGE
import com.flixclusive.domain.provider.util.MediaLinksProviderUtil.EMPTY_PROVIDER_MESSAGE
import com.flixclusive.domain.provider.util.MediaLinksProviderUtil.UNAVAILABLE_EPISODE_MESSAGE
import com.flixclusive.domain.provider.util.MediaLinksProviderUtil.getNoLinksLoadedMessage
import com.flixclusive.domain.provider.util.MediaLinksProviderUtil.isCached
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.provider.CachedLinks
import com.flixclusive.model.provider.MediaLinkResourceState
import com.flixclusive.model.provider.Stream
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmDetails.Companion.isTvShow
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.util.R as UtilR

/**
 *
 * Combination of filmId parameter
 * and season:episode of the film
 * (if it is a tv show).
 *
 * actual format: "$filmId-$season:$episode"
 *
 * */
private typealias FilmKey = String

/**
 *
 * Converts film and episode to [FilmKey].
 * Which is to be used for caching
 *
 * */
private fun getFilmKey(
    filmId: String,
    episodeData: Episode?,
): FilmKey = "$filmId-${episodeData?.season}:${episodeData?.number}"

@Singleton
class GetMediaLinksUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val mediaLinksRepository: MediaLinksRepository,
    private val tmdbRepository: TMDBRepository,
    providerManager: ProviderManager,
) {
    val providerApis: Flow<List<ProviderApi>> = providerManager.workingApis

    /**
     *
     * For caching provider links for each films ONLY
     * because some providers have timeouts on their links
     *
     * */
    val cache = mutableStateMapOf<FilmKey, CachedLinks>()

    /**
     * 
     * Obtains the links of the film and episode.
     * 
     * @param film The film to get the links
     * @param watchHistoryItem The watch history item of the film
     * @param preferredProviderName The name of the preferred provider
     * @param watchId The watch id of the film
     * @param episode The episode of the film if it is a tv show
     * @param runWebView A callback to run the web view
     * @param onSuccess A callback to run when the links are obtained successfully
     * @param onError A callback to run when an error occurs
     * 
     * @return A flow stream of [MediaLinkResourceState]
     * */
    operator fun invoke(
        film: FilmDetails,
        watchHistoryItem: WatchHistoryItem?,
        preferredProviderName: String? = null,
        watchId: String? = null,
        episode: Episode? = null,
        onSuccess: (Episode?) -> Unit,
        onError: ((UiText) -> Unit)? = null,
    ): Flow<MediaLinkResourceState> = channelFlow {
        val apis = getPrioritizedProvidersList(preferredProviderName)

        if (apis.isEmpty()) {
            onError?.invoke(EMPTY_PROVIDER_MESSAGE)
            throwError(EMPTY_PROVIDER_MESSAGE)
            return@channelFlow
        }

        val episodeToUse = when {
            film.isTvShow && episode == null -> {
                sendFetchingEpisodeMessage()
                
                getNearestEpisodeToWatch(
                    film = film as TvShow,
                    watchHistoryItem = watchHistoryItem
                ).also {
                    if (it is Resource.Failure) {
                        onError?.invoke(it.error ?: UNAVAILABLE_EPISODE_MESSAGE)
                        throwError(it.error)
                        return@channelFlow
                    }
                }.data
            }
            else -> episode
        }

        val cachedLinks = getCache(
            filmId = film.identifier,
            episode = episodeToUse
        )

        /*
         *
         * If user chose the same film or the [FilmKey] is valid,
         * then just proceed with the same cached link.
         * */
        if (cachedLinks.isCached(preferredProviderName)) {
            onSuccess(episodeToUse)
            finish()
            return@channelFlow
        }

        for (i in apis.indices) {
            val api = apis[i]

            val isNotTheSameFilmProvider = !film.providerName.equals(api.provider.name, true) && !film.isFromTmdb
            val isChangingProvider = preferredProviderName != null

            if ((isChangingProvider && api.provider.name != preferredProviderName) || isNotTheSameFilmProvider)
                continue

            sendFetchingFilmMessage(provider = api.provider.name)

            if (api.useWebView) {
                storeCache(
                    filmId = film.identifier,
                    episode = episodeToUse,
                    cachedLinks = cachedLinks.copy(
                        watchId = "${film.identifier}-WEBVIEW",
                        providerName = api.provider.name
                    )
                )

                val webView = api.getWebView(context = context)

                sendExtractingLinksMessage(
                    provider = api.provider.name,
                    isOnWebView = true
                )

                try {
                    val links = withContext(ioDispatcher) {
                        webView.getLinks(
                            film = film,
                            episode = episodeToUse,
                        )
                    }
                    webView.destroy()

                    if (links.isEmpty()) {
                        val message = getNoLinksLoadedMessage(api.provider.name)
                        onError?.invoke(message)
                        throwError(message)
                        continue
                    }

                    onSuccess(episodeToUse)
                    trySend(MediaLinkResourceState.Success)
                    return@channelFlow
                } catch (e: Throwable) {
                    webView.destroy()
                    onError?.invoke(UiText.StringValue(e))
                    throwError(e)
                    continue
                }
            }

            val canStopLooping = i == apis.lastIndex || isChangingProvider || !film.isFromTmdb

            val needsNewWatchId = watchId == null && film.isFromTmdb
            val watchIdResource = when {
                needsNewWatchId -> mediaLinksRepository.getWatchId(
                    film = film,
                    api = api
                )
                else -> Resource.Success(watchId ?: film.identifier)
            }

            val hasFailedToGetWatchId = watchIdResource is Resource.Failure || watchIdResource.data.isNullOrBlank()
            when {
                hasFailedToGetWatchId && canStopLooping -> {
                    val error = watchIdResource.error
                        ?: UiText.StringResource(UtilR.string.blank_media_id_error_message)

                    onError?.invoke(error)
                    throwUnavailableError(error)
                    return@channelFlow

                }
                hasFailedToGetWatchId -> continue
            }

            val watchIdToUse = watchIdResource.data!!

            sendExtractingLinksMessage(provider = api.provider.name)
            cachedLinks.streams.clear()
            
            val result = mediaLinksRepository.getLinks(
                film = film,
                watchId = watchIdToUse,
                episode = episodeToUse,
                api = api
            )

            val hasNoStreamLinks = result.data
                ?.filterIsInstance<Stream>()
                ?.isEmpty() ?: false

            when {
                (result is Resource.Failure || hasNoStreamLinks) && canStopLooping -> {
                    val error = when (cachedLinks.streams.size) {
                        0 -> api.provider.name.getNoLinksMessage
                        else -> result.error ?: DEFAULT_ERROR_MESSAGE
                    }

                    onError?.invoke(error)
                    trySend(MediaLinkResourceState.Error(error))
                    return@channelFlow
                }
                result is Resource.Success -> {
                    cachedLinks.addAll(links = result.data ?: emptyList())
                    storeCache(
                        filmId = film.identifier,
                        episode = episodeToUse,
                        cachedLinks = cachedLinks.copy(
                            watchId = watchIdToUse,
                            providerName = api.provider.name
                        )
                    )
                    
                    onSuccess(episodeToUse)
                    trySend(MediaLinkResourceState.Success)
                    return@channelFlow
                }
            }
        }

        trySend(MediaLinkResourceState.Unavailable())
    }

    /**
     * Obtains the [CachedLinks] of the film
     *
     * @param filmId id of the film to fetch
     * @param episode [Episode] of the film if it is a tv show
     *
     * @return [CachedLinks] of the cached film if it exists. Returns a default value if it is null
     * */
    fun getCache(
        filmId: String,
        episode: Episode?,
    ) = cache.getOrElse(getFilmKey(filmId, episode)) {
        CachedLinks()
    }
    
    /**
     * Puts the [CachedLinks] data of the film with its matching cache key
     *
     * @param filmId id of the film to fetch
     * @param episode The episode data of the film if it is a tv show
     * @param cachedLinks The cached links of the cached film
     * */
    private fun storeCache(
        filmId: String,
        episode: Episode?,
        cachedLinks: CachedLinks
    ) = cache.put(getFilmKey(filmId, episode), cachedLinks)

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
                episodeNumber = episodeNumber
            )
        } else {
            val episode = film.seasons.find {
                it.number == seasonNumber
            }?.episodes?.find {
                it.number == episodeNumber
            }

            if (episode == null) {
                return Resource.Failure(UtilR.string.unavailable_episode)
            }

            Resource.Success(episode)
        }
    }

    private suspend fun getEpisodeFromTmdb(
        tmdbId: Int?,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?> {
        return tmdbRepository.getEpisode(
            id = tmdbId ?: return Resource.Failure(UtilR.string.invalid_tmdb_id),
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        ).also {
            if (it is Resource.Failure) {
                return it
            } else if (it is Resource.Success && it.data == null) {
                return Resource.Failure(UtilR.string.unavailable_episode)
            }
        }
    }

    /**
     *
     * Returns a list of providers
     * available that prioritizes the
     * given provider and puts it on top of the list
     * */
    private suspend fun getPrioritizedProvidersList(
        preferredProviderName: String?
    ): List<ProviderApi> {
        if(preferredProviderName != null) {
            return providerApis.first().sortedByDescending {
                it.provider.name.equals(preferredProviderName, true)
            }
        }

        return providerApis.first()
    }

    private val String.getNoLinksMessage
        get() = UiText.StringResource(UtilR.string.no_links_loaded_format_message, this)
}