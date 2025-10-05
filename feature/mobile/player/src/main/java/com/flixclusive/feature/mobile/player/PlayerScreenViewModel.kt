package com.flixclusive.feature.mobile.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.feature.mobile.player.util.MediaLinkUtils.cleanDuplicates
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlayerScreenViewModel @Inject constructor(
    private val appDispatchers: AppDispatchers,
    private val cachedLinksRepository: CachedLinksRepository,
    private val getEpisode: GetEpisodeUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
    private val providerRepository: ProviderRepository,
    private val setWatchProgress: SetWatchProgressUseCase,
    private val userSessionManager: UserSessionManager,
    private val watchProgressRepository: WatchProgressRepository,
    val player: AppPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * Using [SavedStateHandle]'s navArgs delegate to get the navigation arguments.
     * */
    private val navArgs = savedStateHandle.navArgs<PlayerScreenNavArgs>()

    /**
     * The film metadata passed to the player screen.
     * */
    val filmMetadata = navArgs.film

    private val userId: Int
        get() {
            val user = userSessionManager.currentUser.value
            requireNotNull(user) {
                "User must be logged in to use the player"
            }

            return user.id
        }

    private val initialProviderId: String
        get() {
            val currentCache = cachedLinksRepository.currentCache.value
            requireNotNull(currentCache) {
                "Current cache must not be null when initializing the player"
            }

            return currentCache.providerId
        }

    // Only using non-suspend function since we don't need to observe changes here
    val providers by lazy { providerRepository.getEnabledProviders() }

    private val _uiState = MutableStateFlow(PlayerUiState(selectedProvider = initialProviderId))
    val uiState = _uiState.asStateFlow()

    /**
     * Instead of obtaining the selected episode using the non-reactive
     * [SavedStateHandle.get] function, we use a flow to allow for
     * reacting to changes in the selected episode.
     * */
    val selectedEpisode = savedStateHandle
        .getStateFlow<Episode?>("episode", navArgs.episode)

    var nextEpisode: Episode? = null
        private set

    /**
     * The season currently being displayed IF [filmMetadata] is a [TvShow] and a season is selected.
     *
     * This is either the season selected by the user (based on [PlayerUiState.selectedSeason]),
     * the last watched season if no season is selected. If neither of those are available,
     * it will be the latest season.
     *
     * The reason why this is on a separate flow to [PlayerUiState] is because some series
     * have a large number of seasons, and fetching all these seasons can take a while.
     *
     * By separating this into its own flow, we can avoid blocking the entire screen
     * from being displayed while we fetch the season data.
     * */
    val seasonToDisplay = uiState
        .map { it.selectedSeason }
        .filterNotNull()
        .distinctUntilChanged()
        .mapNotNull { selectedSeason ->
            val metadata = filmMetadata
            if (metadata !is TvShow) return@mapNotNull null

            getSeasonWithWatchProgress(metadata, selectedSeason)
        }.flattenConcat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val watchProgress = combine(
        selectedEpisode, // For triggers only
        userSessionManager.currentUser,
    ) { _, user ->
        requireNotNull(user) { "User must be logged in to fetch watch progress inside the Player" }

        watchProgressRepository
            .getAsFlow(
                id = filmMetadata.identifier,
                type = filmMetadata.filmType,
                ownerId = user.id,
            ).filterNotNull()
            .map { it.watchData }
    }.flattenConcat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = createDefaultWatchProgress(),
    )

    private var loadLinksJob: Job? = null

    init {
        initialize()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    /**
     * Called when the user selects a different provider.
     *
     * @param providerId The ID of the selected provider.
     * */
    fun onProviderChange(providerId: String) {
        if (loadLinksJob?.isActive == true) return

        updateWatchProgress()

        loadLinksJob = viewModelScope.launch {
            loadLinks(
                providerId = providerId,
                startPositionMs = player.currentPosition,
                episode = selectedEpisode.value,
                playImmediately = true,
            )
        }
    }

    /**
     * Called when the player auto-queues the next episode to play.
     * */
    fun onQueueNextEpisode() {
        val episode = nextEpisode ?: return

        updateWatchProgress()

        viewModelScope.launch {
            val startPositionMs = getSavedStartPositionMs(episode)

            loadLinks(
                providerId = _uiState.value.selectedProvider,
                startPositionMs = startPositionMs,
                episode = episode,
                playImmediately = false,
                quiet = true,
            )
        }
    }

    fun onSeasonChange(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeason = seasonNumber) }
    }

    fun onAddSubtitle(subtitle: MediaSubtitle) {
        player.addSubtitle(subtitle)
    }

    fun onCancelLoadLinks() {
        loadLinksJob?.cancel()
        _uiState.update { it.copy(loadLinksState = LoadLinksState.Idle) }
    }

    /**
     * Loads the media links for the given [providerId] and prepares the player.
     *
     * @param providerId The ID of the provider to load links from.
     * @param startPositionMs The position to start playback from, in milliseconds.
     * @param episode The episode to load links for, if [filmMetadata] is a [TvShow].
     * @param playImmediately Whether to start playback immediately after loading the links.
     * @param quiet Whether to not show loading/error states in the player.
     *
     * @return True if the links were loaded and the player was prepared, false if the links were already loaded.
     * */
    private suspend fun loadLinks(
        providerId: String,
        startPositionMs: Long,
        episode: Episode?,
        playImmediately: Boolean = false,
        quiet: Boolean = false,
    ): Boolean {
        val cacheKey = CacheKey.create(
            filmId = filmMetadata.identifier,
            providerId = providerId,
            episode = selectedEpisode.value,
        )

        val mediaItemKey = MediaItemKey(
            filmId = filmMetadata.identifier,
            episodeId = selectedEpisode.value?.id,
            providerId = providerId,
        )

        // Check if this media key is already loaded on the player
        val success = player.switchMediaSource(key = mediaItemKey)
        if (success) return true

        // Check if we have cached links for this key
        var cache = cachedLinksRepository.getCache(cacheKey)

        if (cache == null) {
            var areLinksLoaded = false

            val response = when (filmMetadata) {
                is Movie -> {
                    getMediaLinks(
                        movie = filmMetadata,
                        providerId = providerId,
                    )
                }

                is TvShow -> {
                    requireNotNull(episode) {
                        "Selected episode must not be null when loading links for a TV show"
                    }

                    getMediaLinks(
                        tvShow = filmMetadata,
                        episode = episode,
                        providerId = providerId,
                    )
                }

                else -> throw IllegalStateException("Unsupported film type: $filmMetadata")
            }

            response
                .catch { error ->
                    if (quiet) return@catch

                    _uiState.update { it.copy(loadLinksState = LoadLinksState.Error(error)) }
                }.collect { state ->
                    _uiState.update {
                        if (state.isSuccess) {
                            areLinksLoaded = true

                            // Set the current cache in the repository after preparing the player
                            cachedLinksRepository.setCurrentCache(cacheKey)

                            return@update it.copy(
                                selectedProvider = providerId,
                                loadLinksState = if (quiet) it.loadLinksState else LoadLinksState.Idle,
                            )
                        }

                        it.copy(loadLinksState = if (quiet) it.loadLinksState else state)
                    }
                }

            if (!areLinksLoaded) return false

            cache = cachedLinksRepository.getCache(cacheKey) ?: return false
        }

        val servers = cache.streams.cleanDuplicates { index, label ->
            MediaServer(
                label = label,
                url = cache.streams[index].url,
                headers = cache.streams[index].customHeaders,
                source = TrackSource.REMOTE,
            )
        }

        val cleanedSubtitles = cache.subtitles.cleanDuplicates { index, label ->
            MediaSubtitle(
                label = label,
                url = cache.subtitles[index].url,
                source = TrackSource.REMOTE,
            )
        }

        player.prepare(
            key = mediaItemKey,
            servers = servers,
            subtitles = cleanedSubtitles,
            startPositionMs = startPositionMs,
            playImmediately = playImmediately,
        )

        return true
    }

    /**
     * Returns the next episode based on the currently selected episode.
     * If there is no next episode, returns null.
     * */
    private suspend fun getNextEpisode(): Episode? {
        val episode = selectedEpisode.value
        requireNotNull(episode) {
            "Current episode must not be null when getting the next episode"
        }

        return getEpisode(
            tvShow = filmMetadata as TvShow,
            season = episode.season,
            episode = episode.number + 1,
        )
    }

    private fun createDefaultWatchProgress(): WatchProgress {
        return when (filmMetadata) {
            is Movie -> MovieProgress(
                filmId = filmMetadata.identifier,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
            )

            is TvShow -> EpisodeProgress(
                filmId = filmMetadata.identifier,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
                seasonNumber = selectedEpisode.value!!.number,
                episodeNumber = selectedEpisode.value!!.number,
            )

            else -> throw IllegalStateException("Unsupported film type: $filmMetadata")
        }
    }

    /**
     * Updates the current watch progress in the database.
     * */
    private fun updateWatchProgress() {
        appDispatchers.ioScope.launch {
            setWatchProgress(
                watchProgress = when (val progress = watchProgress.value) {
                    is EpisodeProgress -> progress.copy(
                        progress = player.currentPosition,
                        duration = player.duration,
                    )

                    is MovieProgress -> progress.copy(
                        progress = player.currentPosition,
                        duration = player.duration,
                    )
                },
            )
        }
    }

    /**
     * Returns the saved start position in milliseconds for the current film and user.
     * If no saved position exists, returns 0.
     *
     * @param episode The episode to get the start position for, if [filmMetadata] is a [TvShow].
     *
     * @return The saved start position in milliseconds.
     * */
    private suspend fun getSavedStartPositionMs(episode: Episode? = null): Long {
        val watchProgress = if (episode == null) {
            watchProgressRepository
                .get(
                    id = filmMetadata.identifier,
                    type = filmMetadata.filmType,
                    ownerId = userId,
                )?.watchData
        } else {
            watchProgressRepository
                .getSeasonProgress(
                    tvShowId = filmMetadata.identifier,
                    ownerId = userId,
                    seasonNumber = episode.season,
                ).lastOrNull()
        }

        return watchProgress?.progress ?: 0L
    }

    private fun initialize() {
        viewModelScope.launch {
            if (navArgs.episode != null) {
                // Observe changes to the selected episode and load links accordingly
                selectedEpisode.collect { episode ->
                    // Cancel any ongoing link loading for safety
                    onCancelLoadLinks()

                    val startPositionMs = getSavedStartPositionMs(episode)

                    val success = loadLinks(
                        providerId = _uiState.value.selectedProvider,
                        startPositionMs = startPositionMs,
                        episode = episode,
                        playImmediately = true,
                    )

                    // Pre-fetch next episode if available and if the current metadata is a TV show
                    if (success && filmMetadata is TvShow) {
                        nextEpisode = getNextEpisode()
                    }
                }
            } else {
                val startPositionMs = getSavedStartPositionMs()

                // Load links for the movie once
                loadLinks(
                    providerId = _uiState.value.selectedProvider,
                    startPositionMs = startPositionMs,
                    episode = null,
                    playImmediately = true,
                )
            }
        }
    }
}

internal data class PlayerUiState(
    val selectedProvider: String,
    val selectedSeason: Int? = null,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
