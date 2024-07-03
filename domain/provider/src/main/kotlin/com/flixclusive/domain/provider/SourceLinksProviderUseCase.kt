package com.flixclusive.domain.provider

import android.content.Context
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.provider.SourceLinksRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.WebViewCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
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
    private val tmdbRepository: TMDBRepository,
    providersManager: ProviderManager,
) {
    private val defaultErrorMessage = UiText.StringResource(UtilR.string.source_data_dialog_state_error_default)

    companion object {
        /**
         *
         * Converts film and episode to [FilmKey].
         * Which is to be used for caching
         *
         * */
        fun getFilmKey(
            filmId: String,
            episodeData: Episode?,
        ): FilmKey = "$filmId-${episodeData?.season}:${episodeData?.number}"
    }

    val providerApis: Flow<List<ProviderApi>> = providersManager.workingApis

    /**
     *
     * For caching provider links for each films ONLY
     * because some providers have timeouts on their links
     *
     * */
    private val cache = HashMap<FilmKey, SourceData>()

    fun loadLinks(
        film: FilmDetails,
        watchHistoryItem: WatchHistoryItem?,
        preferredProviderName: String? = null,
        watchId: String? = null,
        episode: Episode? = null,
        runWebView: (FlixclusiveWebView) -> Unit,
        onSuccess: (Episode?) -> Unit,
        onError: ((UiText) -> Unit)? = null,
    ): Flow<SourceDataState> = channelFlow {
        val providersList = getPrioritizedProvidersList(preferredProviderName)

        if (providersList.isEmpty()) {
            val emptySourcesErrorId = UtilR.string.no_available_sources

            onError?.invoke(UiText.StringResource(emptySourcesErrorId))
            trySend(SourceDataState.Unavailable(emptySourcesErrorId))
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
                    onError?.invoke(episodeFetchResult.error ?: UiText.StringResource(UtilR.string.unavailable_episode))
                    trySend(SourceDataState.Error(episodeFetchResult.error))
                    return@channelFlow
                }
                Resource.Loading -> null
                is Resource.Success -> episodeFetchResult.data
            }
        } else episode

        val cacheKey = getFilmKey(
            filmId = film.identifier,
            episodeData = episodeToUse
        )

        val cachedSourceData = cache[cacheKey].also { data ->
            val isNewFilm = cache.keys.none {
                it.contains(film.identifier, true)
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

            val isChangingProvider = preferredProviderName != null

            if ((isChangingProvider && provider.name != preferredProviderName))
                continue

            trySend(SourceDataState.Fetching(UiText.StringResource(UtilR.string.fetching_from_provider_format, provider.name)))

            var linksLoaded = 0
            if (provider.useWebView) {
                val webView: FlixclusiveWebView
                val shouldContinue = suspendCoroutine { continuation ->
                    val sourceData = getLinks(
                        filmId = film.identifier,
                        episode = episodeToUse
                    )

                    cache[cacheKey] = sourceData.copy(
                        watchId = "${film.identifier}-WEBVIEW",
                        providerName = provider.name
                    )

                    webView =
                        provider.getWebView(
                            film = film,
                            episode = episodeToUse,
                            context = context,
                            callback = object : WebViewCallback {
                                override suspend fun onSuccess(episode: Episode?) {
                                    if (linksLoaded == 0) {
                                        trySend(SourceDataState.Error(provider.name.getNoLinksMessage))
                                        return
                                    }

                                    trySend(SourceDataState.Success)
                                    onSuccess.invoke(episode)
                                    infoLog("Successful scraping. Destroying WebView...")
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
                                        if (link.url.isBlank()) {
                                            return@onLinkLoaded
                                        }

                                        linksLoaded++
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
                                        onError?.invoke(state.message)
                                        infoLog("Unsuccessful scraping. Destroying WebView...")
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

                return@channelFlow
            }

            val canStopLooping = i == providersList.lastIndex || isChangingProvider

            val isNotTheFilmProvider = !film.providerName.equals(provider.name, true) && !film.isFromTmdb
            val needsNewWatchId = watchId == null && (film.isFromTmdb || isNotTheFilmProvider)

            val watchIdResource = when {
                needsNewWatchId -> sourceLinksRepository.getWatchId(
                    film = film,
                    providerApi = provider
                )
                else -> Resource.Success(watchId ?: film.identifier)
            }

            if (watchIdResource is Resource.Failure || watchIdResource.data.isNullOrBlank()) {
                if (canStopLooping) {
                    val error = watchIdResource.error
                        ?: UiText.StringResource(UtilR.string.blank_media_id_error_message)

                    onError?.invoke(error)
                    trySend(SourceDataState.Unavailable(error))
                    return@channelFlow
                }

                continue
            }

            val watchIdToUse = watchIdResource.data!!

            trySend(SourceDataState.Extracting(UiText.StringResource(UtilR.string.extracting_from_provider_format, provider.name)))

            if (cachedSourceData != null) {
                cachedSourceData.run {
                    /**
                     *
                     * Only clear links since subtitles
                     * could be used on other provider's [SourceData]
                     * */
                    cachedLinks.clear()
                    cache[cacheKey] = copy(
                        watchId = watchIdToUse,
                        providerName = provider.name
                    )
                }
            } else {
                cache[cacheKey] = SourceData(
                    watchId = watchIdToUse,
                    providerName = provider.name
                )
            }

            val sourceData = getLinks(
                filmId = film.identifier,
                episode = episodeToUse
            )

            val result = sourceLinksRepository.getSourceLinks(
                film = film,
                watchId = watchIdToUse,
                episodeData = episodeToUse,
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
                        if (it.url.isBlank()) {
                            return@getSourceLinks
                        }

                        linksLoaded++
                        if (!cachedLinks.contains(it)) {
                            cachedLinks.add(it)
                        }
                    }
                }
            )

            when {
                result is Resource.Failure || linksLoaded == 0 -> {
                    if (canStopLooping) {
                        val error = result.error ?: when (linksLoaded) {
                            0 -> provider.name.getNoLinksMessage
                            else -> defaultErrorMessage
                        }

                        onError?.invoke(error)
                        trySend(SourceDataState.Error(error))
                        return@channelFlow
                    }
                }
                result is Resource.Success -> {
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
     * @param episode [Episode] of the film if it is a tv show
     *
     * @return [SourceData] of the cached film if it exists. Returns a default value if it is null
     * */
    fun getLinks(
        filmId: String,
        episode: Episode?,
    ) = cache.getOrElse(getFilmKey(filmId, episode)) {
        SourceData()
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
                return Resource.Failure(SourceDataState.Unavailable().message)
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
            return providerApis.first()
                .sortedByDescending { it.name.equals(preferredProviderName, true) }
        }

        return providerApis.first()
    }

    val String.getNoLinksMessage
        get() = UiText.StringResource(UtilR.string.no_links_loaded_format_message, this)
}