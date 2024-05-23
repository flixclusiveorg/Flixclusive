package com.flixclusive.domain.provider

import android.content.Context
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.SourceLinksRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.gradle.entities.Status
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.WebViewCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
typealias FilmKey = String

@Singleton
class SourceLinksProviderUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceLinksRepository: SourceLinksRepository,
    private val providersManager: ProviderManager,
    private val providersRepository: ProviderRepository,
    private val tmdbRepository: TMDBRepository,
) {

    companion object {
        /**
         *
         * Converts film and episode to [FilmKey].
         * Which is to be used for caching
         *
         * */
        fun getFilmKey(
            filmId: Int,
            episodeData: TMDBEpisode?,
        ): FilmKey = "$filmId-${episodeData?.season}:${episodeData?.episode}"
    }

     val providerApis: List<ProviderApi>
         get() = providersManager
             .providerDataList
             .flatMap { data ->
                 val api = providersRepository.providers.getValue(data.name)

                 if (
                     data.status != Status.Maintenance
                     && data.status != Status.Down
                     && providersManager.isProviderEnabled(data.name)
                 ) return@flatMap api

                 emptyList()
             }

    /**
     *
     * For caching provider links for each films ONLY
     * because some providers have timeouts on their links
     *
     * */
    private val cache = HashMap<FilmKey, SourceData>()

    fun loadLinks(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        preferredProviderName: String? = null,
        isChangingProvider: Boolean = false,
        mediaId: String? = null,
        episode: TMDBEpisode? = null,
        runWebView: (FlixclusiveWebView) -> Unit,
        onSuccess: (TMDBEpisode?) -> Unit,
        onError: (() -> Unit)? = null,
    ): Flow<SourceDataState> = channelFlow {
        val providersList = getPrioritizedProvidersList(preferredProviderName)

        if (providersList.isEmpty()) {
            onError?.invoke()
            trySend(SourceDataState.Unavailable(UtilR.string.no_available_sources))
            return@channelFlow
        }

        val episodeToUse = if (episode == null && film is TvShow) {
            trySend(SourceDataState.Fetching(UiText.StringResource(UtilR.string.fetching_episode_message)))

            when (
                val episodeFetchResult = getNearestEpisodeToWatch(
                    film = film,
                    watchHistoryItem = watchHistoryItem
                )
            ) {
                is Resource.Failure -> {
                    onError?.invoke()
                    trySend(SourceDataState.Error(episodeFetchResult.error))
                    return@channelFlow
                }

                Resource.Loading -> null
                is Resource.Success -> episodeFetchResult.data
            }
        } else episode

        val cacheKey = getFilmKey(
            filmId = film.id,
            episodeData = episodeToUse
        )

        val cachedSourceData = cache[cacheKey].also { data ->
            val isNewFilm = cache.keys.none {
                it.contains(film.id.toString(), true)
            }
            if(data == null && isNewFilm) {
                return@also cache.clear()
            }

            /**
             *
             * If user chose the same film or the [FilmKey] is valid,
             * then just proceed with the same cached link.
             * */
            if (
                data != null
                && data.cachedLinks.isNotEmpty()
                && (preferredProviderName.equals(data.providerName, true)
                || preferredProviderName == null)
            ) {
                onSuccess(episodeToUse)
                trySend(SourceDataState.Success)
                return@channelFlow
            }
        }

        for (i in providersList.indices) {
            val provider = providersList[i]

            if (provider.name != preferredProviderName && isChangingProvider)
                continue

            trySend(SourceDataState.Fetching(UiText.StringResource(UtilR.string.fetching_from_provider_format, provider.name)))

            if (provider.useWebView) {
                val webView: FlixclusiveWebView
                val shouldContinue = suspendCoroutine { continuation ->
                    val sourceData = getLinks(
                        filmId = film.id,
                        episode = episodeToUse
                    )

                    cache[cacheKey] = sourceData.copy(
                        mediaId = "${film.id}-WEBVIEW",
                        providerName = provider.name
                    )

                    webView =
                        provider.getWebView(
                            film = film,
                            episode = episodeToUse,
                            context = context,
                            callback = object : WebViewCallback {
                                override suspend fun onSuccess(episode: TMDBEpisode?) {
                                    trySend(SourceDataState.Success)
                                    onSuccess.invoke(episode)
                                    debugLog("Destroying WebView...")
                                    continuation.resume(false)
                                }

                                override fun onSubtitleLoaded(subtitle: Subtitle) {
                                    sourceData.run {
                                        if (!cachedSubtitles.contains(subtitle)) {
                                            if (cachedSourceData != null) {
                                                cachedSubtitles.add(0, subtitle)
                                            } else cachedSubtitles.add(subtitle)
                                        }
                                    }
                                }

                                override fun onLinkLoaded(link: SourceLink) {
                                    sourceData.run {
                                        if (!cachedLinks.contains(link)) {
                                            if (cachedSourceData != null) {
                                                cachedLinks.add(0, link)
                                            } else cachedLinks.add(link)
                                        }
                                    }
                                }

                                override suspend fun updateDialogState(state: SourceDataState) {
                                    trySend(state)

                                    if (state is SourceDataState.Error || state is SourceDataState.Unavailable) {
                                        onError?.invoke()
                                        debugLog("Destroying WebView...")
                                        continuation.resume(true)
                                    }
                                }
                            },
                        )!!

                    runWebView(webView)
                }

                webView.destroy()
                if (shouldContinue) {
                    continue
                }
            }

            val canStopLooping = i == providersList.lastIndex
            val needsNewMediaId = mediaId != null && provider.name != preferredProviderName

            val mediaIdResource = if (needsNewMediaId || mediaId == null) {
                sourceLinksRepository.getMediaId(
                    film = film,
                    providerApi = provider
                )
            } else Resource.Success(mediaId)

            if (mediaIdResource is Resource.Failure || mediaIdResource.data.isNullOrBlank()) {
                if (canStopLooping) {
                    onError?.invoke()
                    trySend(SourceDataState.Unavailable(mediaIdResource.error))
                    return@channelFlow
                }

                continue
            }

            val mediaIdToUse = mediaIdResource.data!!

            trySend(SourceDataState.Extracting(UiText.StringResource(UtilR.string.extracting_from_provider_format, provider.name)))

            if (cachedSourceData != null) {
                cachedSourceData.run {
                    /**
                     *
                     * Only clear links since subtitles
                     * could be used on other provider's [SourceData]
                     * */
                    /**
                     *
                     * Only clear links since subtitles
                     * could be used on other provider's [SourceData]
                     * */
                    cachedLinks.clear()
                    cache[cacheKey] = copy(
                        mediaId = mediaIdToUse,
                        providerName = provider.name
                    )
                }
            } else {
                cache[cacheKey] = SourceData(
                    mediaId = mediaIdToUse,
                    providerName = provider.name
                )
            }

            val sourceData = getLinks(
                filmId = film.id,
                episode = episodeToUse
            )

            val result = sourceLinksRepository.getSourceLinks(
                mediaId = mediaIdToUse,
                season = episodeToUse?.season,
                episode = episodeToUse?.episode,
                providerApi = provider,
                onSubtitleLoaded = {
                    sourceData.run {
                        if (!cachedSubtitles.contains(it)) {
                            // If cached data is not null
                            // add the subtitle on the top.
                            if (cachedSourceData != null) {
                                cachedSubtitles.add(0, it)
                            } else cachedSubtitles.add(it)
                        }
                    }
                },
                onLinkLoaded = {
                    sourceData.run {
                        if (!cachedLinks.contains(it)) {
                            cachedLinks.add(it)
                        }
                    }
                }
            )

            when (result) {
                is Resource.Failure -> {
                    if (canStopLooping) {
                        onError?.invoke()
                        trySend(SourceDataState.Error(result.error))
                        return@channelFlow
                    }
                }

                Resource.Loading -> Unit
                is Resource.Success -> {
                    onSuccess(episodeToUse)
                    trySend(SourceDataState.Success)
                    return@channelFlow
                }
            }
        }

        trySend(SourceDataState.Unavailable())
    }

    /**
     *
     *
     * @param filmId id of the film to fetch
     * @param episode [TMDBEpisode] of the film if it is a tv show
     *
     * @return [SourceData] of the cached film if it exists. Returns a default value if it is null
     * */
    fun getLinks(
        filmId: Int,
        episode: TMDBEpisode?,
    ) = cache.getOrElse(getFilmKey(filmId, episode)) {
        SourceData()
    }

    /**
     *
     * Obtains the [TMDBEpisode] data of the nearest
     * episode to be watched by the user
     *
     * */
    private suspend fun getNearestEpisodeToWatch(
        film: TvShow,
        watchHistoryItem: WatchHistoryItem?,
    ): Resource<TMDBEpisode?> {
        val isNewlyWatchShow =
            watchHistoryItem == null || watchHistoryItem.episodesWatched.isEmpty()

        val seasonNumber: Int
        val episodeNumber: Int

        if (isNewlyWatchShow) {
            if (film.totalSeasons == 0 || film.totalEpisodes == 0) {
                return Resource.Failure(SourceDataState.Unavailable().message)
            }

            seasonNumber = 1
            episodeNumber = 1
        } else {
            val (nextSeason, nextEpisode) = getNextEpisodeToWatch(watchHistoryItem!!)
            seasonNumber = nextSeason ?: 1
            episodeNumber = nextEpisode ?: 1
        }

        val episodeFromApiService = tmdbRepository.getEpisode(
            id = film.id,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )

        if (episodeFromApiService is Resource.Failure) {
            return episodeFromApiService
        }
        else if (episodeFromApiService is Resource.Success && episodeFromApiService.data == null) {
            return Resource.Failure(UtilR.string.unavailable_episode)
        }

        return episodeFromApiService
    }


    /**
     *
     * Returns a list of providers
     * available that prioritizes the
     * given provider and puts it on top of the list
     * */
    private fun getPrioritizedProvidersList(
        preferredProviderName: String?
    ): List<ProviderApi> {
        if(preferredProviderName != null) {
            return providerApis
                .sortedByDescending { it.name.equals(preferredProviderName, true) }
        }

        return providerApis
    }
}