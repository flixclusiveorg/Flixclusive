package com.flixclusive.feature.mobile.player

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.presentation.player.AppDataSourceFactory
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer.Companion.getIndexOfPreferredQuality
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinks
import com.flixclusive.data.provider.repository.MediaLinksCacheKey
import com.flixclusive.data.provider.repository.MediaLinksCacheKey.Companion.toCacheKey
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncToScrobblersUseCase
import com.flixclusive.feature.mobile.player.util.MediaLinkUtils.cleanDuplicates
import com.flixclusive.feature.mobile.player.util.MediaLinkUtils.toPlayerServer
import com.flixclusive.feature.mobile.player.util.MediaLinkUtils.toPlayerSubtitle
import com.flixclusive.feature.mobile.player.util.extensions.isSameEpisode
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.tracker.ScrobbleAction
import com.ramcosta.composedestinations.generated.player.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
internal class PlayerScreenViewModel @Inject constructor(
    private val appDispatchers: AppDispatchers,
    private val mediaLinksRepository: MediaLinksRepository,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
    private val providerRepository: ProviderRepository,
    private val setWatchProgress: SetWatchProgressUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    private val dataStoreManager: DataStoreManager,
    private val syncToScrobblers: SyncToScrobblersUseCase,
    private val playerDataSourceFactory: AppDataSourceFactory,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<PlayerScreenNavArgs>()

    private val _scrobblingError = MutableSharedFlow<UiText>()
    val scrobblingError = _scrobblingError.asSharedFlow()

    private var changeProviderJob: Job? = null
    private var changeServerJob: Job? = null
    private var changeEpisodeJob: Job? = null
    private var queueNextEpisodeJob: Job? = null
    private var updateProgressJob: Job? = null
    private var autoQueueNextEpisodeJob: Job? = null

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
        ).also {
            it.initialize()
            it.observePlaybackProgress()
        }
    }

    /**
     * The media metadata passed to the player screen.
     * */
    val mediaMetadata = navArgs.media

    // Only using non-suspend function since we don't need to observe changes here
    val providers = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            providerRepository.getEnabledProvidersAsFlow(ownerId = userId)
                .mapLatest { list ->
                    list.fastMapNotNull { provider ->
                        provider.metadata
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(
        value = PlayerUiState(
            currentProvider = mediaLinksRepository.currentObservable.value?.providerId ?: "",
            currentEpisode = navArgs.episode,
            currentSeason = navArgs.episode?.season,
        )
    )

    val uiState = _uiState.asStateFlow()

    private val distinctEpisodeFlow = _uiState
        .map { it.currentEpisode }
        .distinctUntilChanged()

    private val distinctProviderFlow = _uiState
        .map { it.currentProvider }
        .distinctUntilChanged()

    private val currentMediaLinksCacheKey = distinctProviderFlow
        .combine(distinctEpisodeFlow) { providerId, episode ->
            MediaLinksCacheKey.create(
                mediaId = mediaMetadata.id,
                providerId = providerId,
                episode = episode,
            )
        }

    val servers = currentMediaLinksCacheKey
        .flatMapLatest { cacheKey ->
            mediaLinksRepository.observeLinks(cacheKey)
                .mapLatest { cache ->
                    cache?.streams?.fastMap {
                        it.toPlayerServer()
                    } ?: emptyList()
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mediaLinksRepository.currentObservable.value?.let { cache ->
                cache.streams.fastMap {
                    it.toPlayerServer()
                }
            } ?: emptyList(),
        )

    val failedStreamUrls = currentMediaLinksCacheKey
        .flatMapLatest { cacheKey ->
            mediaLinksRepository.observeLinks(cacheKey)
                .mapLatest { cache ->
                    cache?.failedStreamUrls ?: emptySet()
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mediaLinksRepository.currentObservable.value?.failedStreamUrls ?: emptySet(),
        )

    val canSkipLoading = _uiState
        .map { state ->
            state.loadLinksState.toCacheKey(
                mediaId = mediaMetadata.id,
                episode = state.currentEpisode,
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { cacheKey ->
            if (cacheKey == null) {
                flowOf(false)
            } else {
                mediaLinksRepository.observeLinks(cacheKey)
                    .map { it?.hasStreamableLinks == true }
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val selectedEpisode = distinctEpisodeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = navArgs.episode
        )

    /**
     * The season currently being displayed IF [mediaMetadata] is a [Show] and a season is selected.
     *
     * This is either the season selected by the user (based on [PlayerUiState.currentSeason]),
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
            if (mediaMetadata !is Show) return@mapNotNull null
            it.currentSeason
        }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { selectedSeason ->
            val metadata = mediaMetadata as Show
            getSeasonWithWatchProgress(metadata, selectedSeason)
                .dropWhile { it is Async.Loading }
                .map { state ->
                    when (state) {
                        is Async.Success -> state.data
                        is Async.Failure -> null
                        Async.Loading -> null
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val watchProgress = combine(
        selectedEpisode, // For triggers only
        userSessionDataStore.currentUserId.filterNotNull()
    ) { episode, userId ->
        episode to userId
    }.flatMapLatest { (episode, userId) ->
        watchProgressRepository
            .getAsFlow(
                id = mediaMetadata.id,
                type = mediaMetadata.type,
                ownerId = userId,
            ).filterNotNull()
            .map {
                val progress = it.watchData
                if (progress is EpisodeProgress) {
                    val isSameEpisode = progress.isSameEpisode(
                        otherEpisode = episode?.number ?: -1,
                        otherSeason = episode?.season ?: -1,
                        otherMediaId = mediaMetadata.id,
                    )

                    if (!isSameEpisode) {
                        return@map getDefaultWatchProgress()
                    }
                }

                progress
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = getDefaultWatchProgress(),
    )

    init {
        initialize()
    }

    override fun onCleared() {
        player.release()
        player.releaseMediaSession()
        super.onCleared()
    }

    fun onServerChange(serverIndex: Int) {
        if (changeServerJob?.isActive == true) return

        val mediaLinksCacheKey = MediaLinksCacheKey.create(
            mediaId = mediaMetadata.id,
            providerId = _uiState.value.currentProvider,
            episode = _uiState.value.currentEpisode,
        )

        val cache = mediaLinksRepository.getLinks(mediaLinksCacheKey) ?: return
        if (serverIndex !in cache.streams.indices) return

        _uiState.update { it.copy(currentServer = serverIndex) }
        player.prepare(
            cache = cache,
            startPositionMs = player.currentPosition,
        )
    }

    fun onProviderChange(providerId: String) {
        if (changeProviderJob?.isActive == true) return

        queueNextEpisodeJob?.cancel()
        changeEpisodeJob?.cancel()

        updateWatchProgress()

        val currentServer = _uiState.value.currentServer
        val currentProvider = _uiState.value.currentProvider
        _uiState.update {
            it.copy(currentProvider = providerId, currentServer = -1)
        }

        changeProviderJob = viewModelScope.launch {
            val (key, cache) = loadLinks(
                providerId = providerId,
                episode = _uiState.value.currentEpisode,
            )

            if (cache == null) {
                _uiState.update {
                    it.copy(
                        currentProvider = currentProvider,
                        currentServer = currentServer
                    )
                }
                return@launch
            }

            // TODO: Uncomment this once we support provider changing even if [mediaMetadata] didn't come from TMDb.
            //  The current workaround now is we lock and filter available providers based on the [mediaMetadata.providerId]
            //  if the metadata didn't come from TMDb, but ideally we should be able to switch between providers
            //  even for non-TMDb medias as long as they have support the identifiers of the media (e.g. IMDb ID).

            // if (mediaMetadata is Show) {
            //     val nextEpisode = getNextEpisode(currentEpisode)
            //     _uiState.update { it.copy(nextEpisode = nextEpisode) }
            // }

            mediaLinksRepository.setCurrentObservable(key)
            withContext(appDispatchers.main) {
                player.prepare(
                    cache = cache,
                    startPositionMs = player.currentPosition,
                )
            }
        }

        changeProviderJob?.invokeOnCompletion { throwable ->
            if (throwable != null) {
                _uiState.update {
                    it.copy(
                        currentProvider = currentProvider,
                        currentServer = currentServer
                    )
                }
            }
        }
    }

    fun onSkipProviderLoading() {
        val state = _uiState.value.loadLinksState
        val cacheKey = state.toCacheKey(
            mediaId = mediaMetadata.id,
            episode = selectedEpisode.value,
        ) ?: return

        val cache = mediaLinksRepository.getLinks(cacheKey)
        if (cache == null || !cache.hasStreamableLinks) return

        mediaLinksRepository.setCurrentObservable(cacheKey)

        val providerId = when (state) {
            is LoadLinksState.Extracting -> state.providerId
            is LoadLinksState.Success -> state.providerId
            else -> return
        }

        _uiState.update {
            it.copy(
                currentProvider = providerId,
                loadLinksState = LoadLinksState.Idle,
            )
        }

        viewModelScope.launch(appDispatchers.main) {
            player.prepare(
                cache = cache,
                startPositionMs = player.currentPosition,
            )
        }
    }

    fun onServerFail(serverIndex: Int) {
        val server = servers.value.getOrNull(serverIndex) ?: return
        val mediaLinksCacheKey = MediaLinksCacheKey.create(
            mediaId = mediaMetadata.id,
            providerId = _uiState.value.currentProvider,
            episode = _uiState.value.currentEpisode,
        )

        mediaLinksRepository.markStreamAsFailed(mediaLinksCacheKey, server.url)
    }

    fun onCancelLoading() {
        changeProviderJob?.cancel()
        changeServerJob?.cancel()
        changeEpisodeJob?.cancel()
        queueNextEpisodeJob?.cancel()
        _uiState.update { it.copy(loadLinksState = LoadLinksState.Idle) }
    }

    /**
     * Called when the player auto-queues the next episode to play.
     * */
    private fun onQueueNextEpisode() {
        if (queueNextEpisodeJob?.isActive == true) return

        val episode = _uiState.value.nextEpisode ?: return

        updateWatchProgress()

        queueNextEpisodeJob = viewModelScope.launch {
            loadLinks(
                providerId = _uiState.value.currentProvider,
                episode = episode,
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

            val (key, cache) = loadLinks(
                providerId = _uiState.value.currentProvider,
                episode = episode,
            )

            if (cache != null) {
                mediaLinksRepository.setCurrentObservable(key)
                withContext(appDispatchers.main) {
                    player.prepare(
                        cache = cache,
                        startPositionMs = startPositionMs,
                    )
                }

                _uiState.update {
                    it.copy(
                        currentEpisode = episode,
                        nextEpisode = getNextEpisode(episode)
                    )
                }
            }
        }
    }

    fun onSeasonChange(seasonNumber: Int) {
        _uiState.update { it.copy(currentSeason = seasonNumber) }
    }

    private suspend fun loadLinks(
        providerId: String,
        episode: Episode?,
        quiet: Boolean = false,
    ): Pair<MediaLinksCacheKey, MediaLinks?> {
        val mediaLinksCacheKey = MediaLinksCacheKey.create(
            mediaId = mediaMetadata.id,
            providerId = providerId,
            episode = episode,
        )

        val cache = mediaLinksRepository.getLinks(mediaLinksCacheKey)
        if (cache?.hasExtractedSuccessfully == true) {
            return mediaLinksCacheKey to cache
        }

        val response = getMediaLinks(
            media = mediaMetadata,
            episode = episode,
        )

        response
            .catch { error ->
                if (quiet) return@catch

                _uiState.update { it.copy(loadLinksState = LoadLinksState.Error(error)) }
            }.collect { state ->
                _uiState.update {
                    val hasSkippedLoading = state.isError && it.loadLinksState.isIdle
                    if (hasSkippedLoading && !quiet) {
                        return@update it
                    }

                    if (state.isSuccess) {
                        return@update it.copy(
                            currentProvider = providerId,
                            loadLinksState = if (quiet) it.loadLinksState else LoadLinksState.Idle,
                        )
                    }

                    it.copy(loadLinksState = if (quiet) it.loadLinksState else state)
                }
            }

        return mediaLinksCacheKey to mediaLinksRepository.getLinks(mediaLinksCacheKey)
    }

    @MainThread
    private fun AppPlayer.prepare(
        cache: MediaLinks,
        startPositionMs: Long,
    ) {
        val servers = cache.streams.cleanDuplicates {
            (it as Stream).toPlayerServer()
        }
        val subtitles = cache.subtitles.cleanDuplicates {
            (it as Subtitle).toPlayerSubtitle()
        }

        var currentServer = _uiState.value.currentServer
        if (currentServer !in servers.indices) {
            currentServer = servers.getIndexOfPreferredQuality(playerPreferences.value.quality)

            _uiState.update { it.copy(currentServer = currentServer) }
        }

        prepare(
            server = servers[currentServer],
            subtitles = subtitles,
            startPositionMs = startPositionMs,
        )

        updateWatchProgress()
    }

    /**
     * Returns the next episode based on the currently selected episode.
     * If there is no next episode, returns null.
     * */
    private suspend fun getNextEpisode(episode: Episode?): Episode? {
        if (episode == null) return null

        return getNextEpisode(
            show = mediaMetadata as Show,
            season = episode.season,
            episode = episode.number,
        )
    }

    private fun getDefaultWatchProgress(): WatchProgress {
        // Careful here - reconsider this in the future
        val userId = runBlocking { userSessionDataStore.currentUserId.filterNotNull().first() }

        return when (mediaMetadata) {
            is Movie -> MovieProgress(
                mediaId = mediaMetadata.id,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
            )

            is Show -> EpisodeProgress(
                mediaId = mediaMetadata.id,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
                seasonNumber = selectedEpisode.value!!.season,
                episodeNumber = selectedEpisode.value!!.number,
            )

            else -> throw IllegalStateException("Unsupported media type: $mediaMetadata")
        }
    }

    @OptIn(UnstableApi::class)
    private fun AppPlayer.observePlaybackProgress() {
        viewModelScope.launch(appDispatchers.main) {
            launch {
                listenTo(Player.EVENT_PLAYBACK_STATE_CHANGED) { events ->
                    if (!events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED))
                        return@listenTo

                    val isFinished = !isPlaying && currentPosition >= duration && duration > 0
                    val nextEpisode = _uiState.value.nextEpisode
                    if (isFinished && nextEpisode != null) {
                        onEpisodeChange(nextEpisode)
                        return@listenTo
                    }
                }
            }

            launch {
                listenTo(Player.EVENT_IS_PLAYING_CHANGED) { events ->
                    if (!events.contains(Player.EVENT_IS_PLAYING_CHANGED))
                        return@listenTo

                    updateWatchProgress()

                    autoQueueNextEpisodeJob?.cancel()
                    autoQueueNextEpisodeJob = launch {
                        while (isPlaying) {
                            if (duration <= 0) continue

                            val isQueueingNextEpisode = currentPosition >= (duration * QUEUE_THRESHOLD)
                            if (navArgs.media is Show && isQueueingNextEpisode) {
                                onQueueNextEpisode()
                            }

                            delay(3.seconds)
                        }
                    }
                }
            }
        }
    }

    fun updateWatchProgress() {
        if (updateProgressJob?.isActive == true) {
            updateProgressJob?.cancel()
        }

        updateProgressJob = appDispatchers.ioScope.launch {
            val currentPosition = withContext(appDispatchers.main) {
                player.currentPosition
            }
            val duration = withContext(appDispatchers.main) {
                player.duration
            }

            val canSaveProgress = currentPosition > 60_000L
            if (!canSaveProgress) return@launch

            val progress = when (val progress = watchProgress.value) {
                is EpisodeProgress -> progress.copy(
                    progress = currentPosition,
                    duration = duration,
                    status = WatchStatus.WATCHING,
                    updatedAt = Date()
                )

                is MovieProgress -> progress.copy(
                    progress = currentPosition,
                    duration = duration,
                    status = WatchStatus.WATCHING,
                    updatedAt = Date()
                )
            }

            setWatchProgress(
                media = mediaMetadata,
                watchProgress = progress,
            )

            // Adding a small debounce here to ensure that if the user quickly changes providers/episodes,
            // we don't end up sending multiple scrobble events in quick succession which can cause issues/
            delay(1500L)

            val isPlaying = withContext(appDispatchers.main) {
                player.isPlaying
            }

            try {
                syncToScrobblers(
                    action = if (isPlaying) ScrobbleAction.START else ScrobbleAction.STOP,
                    media = mediaMetadata,
                    episode = selectedEpisode.value,
                    watchProgress = progress,
                )
            } catch (e: Throwable) {
                _scrobblingError.emit(UiText.from(e.message ?: "Unknown scrobbling error"))
            }
        }
    }

    /**
     * Returns the saved start position in milliseconds for the current media and user.
     * If no saved position exists, returns 0.
     *
     * @param episode The episode to get the start position for, if [mediaMetadata] is a [Show].
     *
     * @return The saved start position in milliseconds.
     * */
    private suspend fun getSavedStartPositionMs(episode: Episode? = null): Long {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val watchProgress = if (episode == null) {
            watchProgressRepository.get(
                id = mediaMetadata.id,
                type = mediaMetadata.type,
                ownerId = userId,
            )?.watchData
        } else {
            watchProgressRepository.getEpisodeProgress(
                tvShowId = mediaMetadata.id,
                seasonNumber = episode.season,
                episodeNumber = episode.number,
                ownerId = userId,
            )
        }

        if (watchProgress?.status == WatchStatus.COMPLETED) {
            return 0L
        }

        return watchProgress?.progress ?: 0L
    }

    private fun initialize() {
        viewModelScope.launch {
            val mediaLinksCacheKey = MediaLinksCacheKey.create(
                mediaId = mediaMetadata.id,
                providerId = _uiState.value.currentProvider,
                episode = _uiState.value.currentEpisode,
            )

            val cache = mediaLinksRepository.getLinks(mediaLinksCacheKey)
            if (cache == null || !cache.hasStreamableLinks) {
                return@launch
            }

            mediaLinksRepository.setCurrentObservable(mediaLinksCacheKey)
            val nextEpisode = getNextEpisode(navArgs.episode)
            _uiState.update {
                it.copy(nextEpisode = nextEpisode)
            }

            withContext(appDispatchers.main) {
                player.prepare(
                    cache = cache,
                    startPositionMs = getSavedStartPositionMs(navArgs.episode),
                )
            }
        }
    }
}

// TODO: Make this threshold configurable in the future, maybe even allow users to set it themselves.
//  For now, 80% seems like a reasonable default that allows enough time for links to load without cutting off too early.
private const val QUEUE_THRESHOLD = 0.8

@Immutable
internal data class PlayerUiState(
    val currentProvider: String = "",
    val currentSeason: Int? = null,
    val currentEpisode: Episode? = null,
    val currentServer: Int = -1,
    val nextEpisode: Episode? = null,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
