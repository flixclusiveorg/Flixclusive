package com.flixclusive.feature.mobile.player

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.player.AppDataSourceFactory
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.PlayerErrorReceiver
import com.flixclusive.core.presentation.player.extensions.getDisplayMessage
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
import com.ramcosta.composedestinations.generated.player.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private val dataStoreManager: DataStoreManager,
    private val playerDataSourceFactory: AppDataSourceFactory,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<PlayerScreenNavArgs>()


    private val _playerErrors = MutableSharedFlow<UiText>(extraBufferCapacity = 5)
    val playerErrors: SharedFlow<UiText> = _playerErrors.asSharedFlow()

    private val errorConsumer by lazy { PlayerErrorConsumer(_playerErrors) }

    val playerPreferences = dataStoreManager.getUserPrefs(
        key = UserPreferences.PLAYER_PREFS_KEY,
        type = PlayerPreferences::class,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking {
            dataStoreManager.getUserPrefs(
                key = UserPreferences.PLAYER_PREFS_KEY,
                type = PlayerPreferences::class,
            ).first()
        },
    )

    val subtitlesPreferences = dataStoreManager.getUserPrefs(
        key = UserPreferences.SUBTITLES_PREFS_KEY,
        type = SubtitlesPreferences::class,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking {
            dataStoreManager.getUserPrefs(
                key = UserPreferences.SUBTITLES_PREFS_KEY,
                type = SubtitlesPreferences::class,
            ).first()
        },
    )

    val player by lazy {
        AppPlayer(
            context = context,
            dataSourceFactory = playerDataSourceFactory,
            playerPrefs = playerPreferences.value,
            subtitlePrefs = subtitlesPreferences.value,
            errorReceiver = errorConsumer,
        ).also {
            it.initialize()
            it.observePlaybackProgress()
        }
    }

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

    private val _uiState = MutableStateFlow(
        value = PlayerUiState(
            selectedProvider = initialProviderId,
            selectedEpisode = navArgs.episode,
            selectedSeason = navArgs.episode?.season,
        )
    )
    val uiState = _uiState.asStateFlow()

    val selectedEpisode = _uiState
        .map { it.selectedEpisode }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = navArgs.episode
        )

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
        .mapNotNull {
            if (filmMetadata !is TvShow) return@mapNotNull null
            it.selectedSeason
        }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { selectedSeason ->
            val metadata = filmMetadata as TvShow
            getSeasonWithWatchProgress(metadata, selectedSeason)
                .dropWhile { it is Resource.Loading }
                .map { it.data }
        }
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

    private var changeProviderJob: Job? = null
    private var changeEpisodeJob: Job? = null
    private var queueNextEpisodeJob: Job? = null
    private var updateProgressJob: Job? = null

    init {
        initialize()
    }

    override fun onCleared() {
        player.release()
        player.releaseMediaSession()
        super.onCleared()
    }

    fun onProviderChange(providerId: String) {
        if (changeProviderJob?.isActive == true) return

        queueNextEpisodeJob?.cancel()
        changeEpisodeJob?.cancel()

        updateWatchProgress()

        changeProviderJob = viewModelScope.launch {
            loadLinks(
                providerId = providerId,
                startPositionMs = withContext(appDispatchers.main) {
                    player.currentPosition
                },
                episode = selectedEpisode.value,
                playImmediately = true,
            )
        }
    }

    /**
     * Called when the player auto-queues the next episode to play.
     * */
    fun onQueueNextEpisode() {
        if (queueNextEpisodeJob?.isActive == true) return

        val episode = _uiState.value.nextEpisode ?: return

        updateWatchProgress()

        queueNextEpisodeJob = viewModelScope.launch {
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

    fun onEpisodeChange(episode: Episode) {
        if (changeEpisodeJob?.isActive == true) return

        queueNextEpisodeJob?.cancel()
        changeProviderJob?.cancel()

        updateWatchProgress()

        changeEpisodeJob = viewModelScope.launch {
            val startPositionMs = getSavedStartPositionMs(episode)

            val success = loadLinks(
                providerId = _uiState.value.selectedProvider,
                startPositionMs = startPositionMs,
                episode = episode,
                playImmediately = true,
            )

            if (success) {
                _uiState.update {
                    it.copy(
                        selectedEpisode = episode,
                        nextEpisode = getNextEpisode(episode)
                    )
                }
            }
        }
    }

    fun onSeasonChange(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeason = seasonNumber) }
    }

    fun onAddSubtitle(subtitle: MediaSubtitle) {
        player.addSubtitle(subtitle)
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
            episode = episode,
        )

        val mediaItemKey = MediaItemKey(
            filmId = filmMetadata.identifier,
            episodeId = episode?.id,
            providerId = providerId,
        )

        val success = withContext(appDispatchers.main) {
            when {
                playImmediately -> player.switchMediaSource(
                    key = mediaItemKey,
                    startPositionMs = startPositionMs
                )
                else -> player.hasMediaSource(key = mediaItemKey)
            }
        }
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

        if (playImmediately) {
            cachedLinksRepository.setCurrentCache(cacheKey)
        }

        withContext(appDispatchers.main) {
            player.prepare(
                key = mediaItemKey,
                servers = servers,
                subtitles = cleanedSubtitles,
                startPositionMs = startPositionMs,
                playImmediately = playImmediately,
            )
        }

        return true
    }

    /**
     * Returns the next episode based on the currently selected episode.
     * If there is no next episode, returns null.
     * */
    private suspend fun getNextEpisode(episode: Episode?): Episode? {
        if (episode == null) return null

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

    private fun AppPlayer.observePlaybackProgress() {
        viewModelScope.launch(appDispatchers.main) {
            listenTo(Player.EVENT_PLAYBACK_STATE_CHANGED) { events ->
                if (!events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED))
                    return@listenTo

                val isFinished = !isPlaying && currentPosition >= duration
                val nextEpisode = _uiState.value.nextEpisode
                if (isFinished && nextEpisode != null) {
                    onEpisodeChange(nextEpisode)
                    return@listenTo
                }

                if (filmMetadata is TvShow) {
                    if (duration <= 0) return@listenTo

                    if (currentPosition >= (duration * QUEUE_THRESHOLD)) {
                        onQueueNextEpisode()
                    }
                }
            }
        }
    }

    fun updateWatchProgress() {
        if (updateProgressJob?.isActive == true) return

        updateProgressJob = appDispatchers.ioScope.launch {
            val currentPosition = withContext(appDispatchers.main) {
                player.currentPosition
            }
            val duration = withContext(appDispatchers.main) {
                player.duration
            }

            val canSaveProgress = currentPosition > 60_000L
            if (!canSaveProgress) return@launch

            setWatchProgress(
                film = filmMetadata,
                watchProgress = when (val progress = watchProgress.value) {
                    is EpisodeProgress -> progress.copy(
                        progress = currentPosition,
                        duration = duration,
                    )

                    is MovieProgress -> progress.copy(
                        progress = currentPosition,
                        duration = duration,
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
                changeProviderJob?.cancel()
                changeEpisodeJob?.cancel()
                queueNextEpisodeJob?.cancel()
                _uiState.update { it.copy(loadLinksState = LoadLinksState.Idle) }

                val startPositionMs = getSavedStartPositionMs(navArgs.episode)

                val success = loadLinks(
                    providerId = _uiState.value.selectedProvider,
                    startPositionMs = startPositionMs,
                    episode = navArgs.episode,
                    playImmediately = true,
                )

                // Pre-fetch next episode if available and if the current metadata is a TV show
                if (success && filmMetadata is TvShow) {
                    val nextEpisode = getNextEpisode(navArgs.episode)
                    _uiState.update {
                        it.copy(nextEpisode = nextEpisode)
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

// TODO: Make this threshold configurable in the future, maybe even allow users to set it themselves.
//  For now, 80% seems like a reasonable default that allows enough time for links to load without cutting off too early.
private const val QUEUE_THRESHOLD = 0.8

private class PlayerErrorConsumer(
    private val errorFlow: MutableSharedFlow<UiText>,
) : PlayerErrorReceiver {
    override fun onPlayerError(error: PlaybackException) {
        errorFlow.tryEmit(error.getDisplayMessage())
    }
}

@Immutable
internal data class PlayerUiState(
    val selectedProvider: String,
    val selectedSeason: Int? = null,
    val selectedEpisode: Episode? = null,
    val nextEpisode: Episode? = null,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
