package com.flixclusive.domain.provider

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.data.provider.MediaLinksRepository
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.ProviderRepository
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
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink.Companion.getOrNull
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.ProviderWebViewApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

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
class GetMediaLinksUseCase
    @Inject
    constructor(
        private val mediaLinksRepository: MediaLinksRepository,
        private val tmdbRepository: TMDBRepository,
        private val providerApiRepository: ProviderApiRepository,
        private val providerRepository: ProviderRepository,
    ) {
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

                val cachedLinks =
                    getCache(
                        filmId = film.identifier,
                        episode = episodeToUse,
                    )

                if (apis.isEmpty()) {
                    if (film.tmdbId != null) {
                        val response =
                            loadOfficialWatchProvidersFromTmdb(
                                cachedLinks = cachedLinks,
                                film = film,
                                episode = episode,
                            )

                        if (response is Resource.Success) {
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

                clearTrustedCache(cachedLinks)

                if (cachedLinks.isCached(preferredProvider)) {
                    onSuccess(episodeToUse)
                    finish()
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

                    cachedLinks.streams.clear()
                    storeCache(
                        filmId = film.identifier,
                        episode = episodeToUse,
                        cachedLinks =
                            cachedLinks.copy(
                                watchId = watchIdToUse,
                                providerId = apiId,
                            ),
                    )

                    val result =
                        mediaLinksRepository.getLinks(
                            film = film,
                            watchId = watchIdToUse,
                            episode = episodeToUse,
                            api = api,
                            onLinkFound = cachedLinks::add,
                        )

                    val noLinksLoaded = result is Resource.Success && cachedLinks.streams.isEmpty()
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
            cachedLinks: CachedLinks,
        ) = cache.put(getFilmKey(filmId, episode), cachedLinks)

        private fun clearTrustedCache(cachedLinks: CachedLinks) {
            cachedLinks.streams.removeIf {
                it.flags?.getOrNull(Flag.Trusted::class) != null
            }
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
            var providers = providerRepository.getProviders()
            if (preferredProvider != null) {
                providers = providers.sortedByDescending {
                    it.id.equals(preferredProvider, true)
                }
            }

            return providers
                .mapNotNull { provider ->
                    providerApiRepository.getApi(provider.id)?.let { api ->
                        ProviderApiWithId(
                            id = provider.id,
                            api = api
                        )
                    }
                }
        }

        private suspend fun getCorrectWatchId(
            watchId: String?,
            film: FilmMetadata,
            api: ProviderApi,
        ): Resource<String?> {
            val needsNewWatchId = watchId == null && film.isFromTmdb && api !is ProviderWebViewApi
            val watchIdResource =
                when {
                    needsNewWatchId -> api.getWatchId(film = film)
                    else -> Resource.Success(watchId ?: film.identifier)
                }

            return watchIdResource
        }

        private suspend fun loadOfficialWatchProvidersFromTmdb(
            cachedLinks: CachedLinks,
            film: FilmMetadata,
            episode: Episode?,
        ): Resource<Unit> {
            val response =
                tmdbRepository.getWatchProviders(
                    mediaType = film.filmType.type,
                    id = film.tmdbId!!,
                )

            response.data?.fastForEach(cachedLinks::add)
            storeCache(
                filmId = film.identifier,
                episode = episode,
                cachedLinks =
                    cachedLinks.copy(
                        watchId = film.identifier,
                        providerId = DEFAULT_FILM_SOURCE_NAME,
                    ),
            )

            return if (response is Resource.Failure) {
                Resource.Failure(response.error)
            } else {
                Resource.Success(Unit)
            }
        }
    }

private data class ProviderApiWithId(
    val id: String,
    val api: ProviderApi,
)
