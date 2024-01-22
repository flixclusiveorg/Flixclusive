package com.flixclusive.domain.provider

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.SourceLinksRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.provider.SourceData
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.base.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
typealias FilmKey = String

@Singleton
class SourceLinksProviderUseCase @Inject constructor(
    private val sourceLinksRepository: SourceLinksRepository,
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

     val providers: List<Provider>
        get() = providersRepository.providers
            .filter { !it.isIgnored && !it.isMaintenance }
            .map { it.provider }

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
        onSuccess: (TMDBEpisode?) -> Unit,
        onError: (() -> Unit)? = null,
    ): Flow<SourceDataState> = flow {
        val providersList = getPrioritizedProvidersList(preferredProviderName)

        if (providersList.isEmpty()) {
            onError?.invoke()
            return@flow emit(SourceDataState.Unavailable(UtilR.string.no_available_sources))
        }

        val episodeToUse = if (episode == null && film is TvShow) {
            emit(SourceDataState.Fetching(UiText.StringResource(UtilR.string.fetching_episode_message)))

            when (
                val episodeFetchResult = getNearestEpisodeToWatch(
                    film = film,
                    watchHistoryItem = watchHistoryItem
                )
            ) {
                is Resource.Failure -> {
                    onError?.invoke()
                    return@flow emit(SourceDataState.Error(episodeFetchResult.error))
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
                return@flow emit(SourceDataState.Success)
            }
        }

        for (i in providersList.indices) {
            val provider = providersList[i]

            if (provider.name != preferredProviderName && isChangingProvider)
                continue

            emit(SourceDataState.Fetching(UiText.StringResource(UtilR.string.fetching_from_provider_format, provider.name)))

            val canStopLooping = i == providersList.lastIndex
            val needsNewMediaId = mediaId != null && provider.name != preferredProviderName

            val mediaIdToUse = if (needsNewMediaId || mediaId == null) {
                sourceLinksRepository.getMediaId(
                    film = film,
                    provider = provider
                )
            } else mediaId

            if (mediaIdToUse.isNullOrEmpty()) {
                if (canStopLooping) {
                    onError?.invoke()
                    return@flow emit(SourceDataState.Unavailable())
                }

                continue
            }

            emit(SourceDataState.Extracting(UiText.StringResource(UtilR.string.extracting_from_provider_format, provider.name)))

            if (cachedSourceData != null) {
                cachedSourceData.run {
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
                provider = provider,
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
                        return@flow emit(SourceDataState.Error(result.error))
                    }
                }

                Resource.Loading -> Unit
                is Resource.Success -> {
                    onSuccess(episodeToUse)
                    return@flow emit(SourceDataState.Success)
                }
            }
        }

        emit(SourceDataState.Unavailable())
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
    ): List<Provider> {
        if(preferredProviderName != null) {
            return providers
                .sortedByDescending { it.name.equals(preferredProviderName, true) }
        }

        return providers
    }
}