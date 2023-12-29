package com.flixclusive.data.usecase

import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.SourceData
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.ProvidersRepository
import com.flixclusive.domain.repository.SourceLinksRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.SourceLinksProviderUseCase
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.utils.LoggerUtils.debugLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 *
 * Combination of filmId parameter
 * and season:episode of the film,
 * if it is a tv show.
 *
 * actual format: "$filmId-$season:$episode"
 *
 * */
typealias FilmKey = String

class SourceLinksProviderUseCaseImpl @Inject constructor(
    private val sourceLinksRepository: SourceLinksRepository,
    private val providersRepository: ProvidersRepository,
    private val tmdbRepository: TMDBRepository,
) : SourceLinksProviderUseCase {

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

    override val providers: List<SourceProvider>
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

    override fun loadLinks(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        preferredProviderName: String?,
        isChangingProvider: Boolean,
        mediaId: String?,
        episode: TMDBEpisode?,
        onSuccess: (TMDBEpisode?) -> Unit,
        onError: (() -> Unit)?,
    ): Flow<SourceDataState> = flow {
        val providesList = getPrioritizedProvidersList(preferredProviderName)

        if(providesList.isEmpty()) {
            onError?.invoke()
            return@flow emit(SourceDataState.Unavailable(R.string.no_available_sources))
        }

        val episodeToUse = if(episode == null && film is TvShow) {
            emit(SourceDataState.Fetching(UiText.StringResource(R.string.fetching_episode_message)))

            when(
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
        val cacheKey = getFilmKey(film.id, episodeToUse)
        
        val cachedSourceData = cache[cacheKey]?.let { cache ->
            // If user chose the same provider then just skip all the process
            // Otherwise, clear the links and fetch for a new provider
            if(preferredProviderName == cache.sourceNameUsed)
                return@flow onSuccess(episodeToUse)

            cache
        }

        for(i in providesList.indices) {
            val provider = providesList[i]

            if(provider.name != preferredProviderName && isChangingProvider)
                continue

            emit(SourceDataState.Fetching("Fetching from ${provider.name}..."))

            val canStopLooping = i == providesList.lastIndex
            val needsNewMediaId = mediaId != null && provider.name != preferredProviderName

            val mediaIdToUse = if(needsNewMediaId || mediaId == null) {
                sourceLinksRepository.getMediaId(
                    film = film,
                    provider = provider
                )
            } else mediaId

            if (mediaIdToUse.isNullOrEmpty()) {
                if(canStopLooping) {
                    onError?.invoke()
                    return@flow emit(SourceDataState.Unavailable())
                }

                continue
            }

            emit(SourceDataState.Extracting("Extracting from ${provider.name}..."))

            if(cachedSourceData != null) {
                cachedSourceData.run {
                    /**
                     *
                     * Only clear links since subtitles
                     * could be used on other provider's [SourceLink]
                     * */
                    cachedLinks.clear()
                    cache[cacheKey] = copy(
                        mediaId = mediaIdToUse,
                        sourceNameUsed = provider.name
                    )
                }
            } else {
                cache[cacheKey] = SourceData(
                    mediaId = mediaIdToUse,
                    sourceNameUsed = provider.name
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
                        if(!cachedSubtitles.contains(it)) {
                            debugLog("Adding subs [${cachedSubtitles.size}]")

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
                        if(!cachedLinks.contains(it)) {
                            debugLog("Adding links [${cachedLinks.size}]")
                            cachedLinks.add(it)
                        }
                    }
                }
            )

            when (result) {
                is Resource.Failure -> {
                    if(canStopLooping) {
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
    override fun getLinks(
        filmId: Int,
        episode: TMDBEpisode?,
    ) = cache.getOrElse(getFilmKey(filmId, episode)) {
        SourceData()
    }

    override fun clearCache() {
        cache.clear()
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
            val (nextSeason, nextEpisode) =
                WatchHistoryUtils.getNextEpisodeToWatch(watchHistoryItem!!)
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
            return Resource.Failure(R.string.unavailable_episode)
        }

        return episodeFromApiService
    }


    /**
     *
     * Returns a list of providers
     * available that prioritizes the
     * given provider and on top of the list
     * */
    private fun getPrioritizedProvidersList(
        preferredProvider: String?
    ): List<SourceProvider> {
        if(preferredProvider != null) {
            return providers
                .sortedBy { it.name == preferredProvider }
        }

        return providers
    }
}