package com.flixclusive.feature.mobile.player

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
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
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefs
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.PlaybackRequest
import com.flixclusive.core.presentation.player.AppDataSourceFactory
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.SetWatchProgressUseCase
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.GetCompletedDownloadFileUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncToScrobblersUseCase
import com.flixclusive.domain.provider.util.LinkMatcher.getIndexOfPreferredQuality
import com.flixclusive.feature.mobile.player.util.extensions.isSameEpisode
import com.flixclusive.feature.mobile.player.util.extensions.toEpisode
import com.flixclusive.feature.mobile.player.util.extensions.toFallbackMedia
import com.flixclusive.feature.mobile.player.util.extensions.toPlayerServer
import com.flixclusive.feature.mobile.player.util.extensions.toPlayerServers
import com.flixclusive.feature.mobile.player.util.extensions.toPlayerSubtitles
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.tracker.ScrobbleAction
import com.ramcosta.composedestinations.generated.player.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@kotlin.OptIn(FlowPreview::class)
@HiltViewModel
internal class PlayerScreenViewModel @Inject constructor(
    private val appDispatchers: AppDispatchers,
    private val mediaLinksRepository: MediaLinksRepository,
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val getCompletedDownloadFile: GetCompletedDownloadFileUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
    private val providerRepository: ProviderRepository,
    private val getProviderMetadata: GetProviderMetadataUseCase,
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

    /** The [PlaybackRequest.FromProvider] request, when [navArgs] declares provider playback. */
    private val providerRequest: PlaybackRequest.FromProvider?
        get() = navArgs.request as? PlaybackRequest.FromProvider

    /** Non-null only for [PlaybackRequest.FromDownload]-driven playback. Resolved
     * synchronously — like the two DataStore-prefs [runBlocking] reads below — so [media] stays
     * non-null and every one of its existing readers is untouched by local playback. */
    private data class LocalPlayback(
        val item: DownloadItem,
        val file: CompletedDownloadFile?,
        val media: MediaMetadata,
        val episode: Episode?,
    )

    private val localPlayback: LocalPlayback? =
        (navArgs.request as? PlaybackRequest.FromDownload)?.let { request ->
            runBlocking {
                val item = mediaDownloadRepository.getItem(request.downloadItemId) ?: return@runBlocking null
                LocalPlayback(
                    item = item,
                    file = getCompletedDownloadFile(item),
                    media = mediaLinksRepository.getMedia(item.mediaId) ?: item.toFallbackMedia(),
                    episode = item.toEpisode(),
                )
            }
        }

    private val isLocalPlayback get() = localPlayback != null

    private val _playerErrors = MutableSharedFlow<UiText>()
    val playerErrors = _playerErrors.asSharedFlow()

    private var changeProviderJob: Job? = null
    private var changeServerJob: Job? = null
    private var changeEpisodeJob: Job? = null
    private var queueNextEpisodeJob: Job? = null
    private var updateProgressJob: Job? = null
    private var autoQueueNextEpisodeJob: Job? = null

    /** Current user ID, resolved once at startup (Eagerly). */
    private val ownerId = userSessionDataStore.currentUserId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val playerPreferences = dataStoreManager
        .getUserPrefsAsFlow<PlayerPreferences>(key = UserPreferences.PLAYER_PREFS_KEY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager
                    .getUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)
            },
        )

    val subtitlesPreferences = dataStoreManager
        .getUserPrefsAsFlow<SubtitlesPreferences>(key = UserPreferences.SUBTITLES_PREFS_KEY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                dataStoreManager
                    .getUserPrefs<SubtitlesPreferences>(UserPreferences.SUBTITLES_PREFS_KEY)
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

    /** The media actually being played — resolved locally from [localPlayback] for a download,
     * [PlaybackRequest.FromProvider.media] otherwise. Exposed (not private) because
     * [PlayerScreen] needs the resolved, guaranteed-non-null value to render. */
    val media: MediaMetadata
        get() = localPlayback?.media
            ?: providerRequest?.media
            // Unreachable: DownloadsTweakViewModel.onOpen gates on the file resolving before
            // navigating a FromDownload request, so mediaDownloadRepository.getItem() above
            // cannot fail in practice.
            ?: error("PlaybackRequest.FromDownload failed to resolve local playback")

    private val _providers = MutableStateFlow<Async<List<ProviderMetadata>>>(Async.Loading)
    val providers = _providers.asStateFlow()

    private val _servers = MutableStateFlow<Async<List<PlayerServer>>>(Async.Loading)
    val servers = _servers.asStateFlow()

    /** The episode being played — [LocalPlayback.episode] resolved from the [DownloadItem] for
     * local playback, [PlaybackRequest.FromProvider.episode] otherwise. */
    private val initialEpisode = localPlayback?.episode ?: providerRequest?.episode

    private val _uiState = MutableStateFlow(
        value = PlayerUiState(
            currentProvider = media.providerId,
            currentEpisode = initialEpisode,
            currentSeason = initialEpisode?.season,
        )
    )

    val uiState = _uiState.asStateFlow()

    val selectedEpisode = _uiState
        .map { it.currentEpisode }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialEpisode
        )

    val canSkipLoading = servers
        .debounce(600.milliseconds)
        .mapLatest { (it as? Async.Success)?.data }
        .mapLatest { it?.isNotEmpty() == true }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val seasonToDisplay = uiState
        .mapNotNull {
            if (media !is Show) return@mapNotNull null
            it.currentSeason
        }.distinctUntilChanged()
        .flatMapLatest { selectedSeason ->
            val metadata = media as Show
            getSeasonWithWatchProgress(metadata, selectedSeason)
                .dropWhile { it is Async.Loading }
                .map { state ->
                    when (state) {
                        is Async.Success -> state.data
                        is Async.Failure -> null
                        Async.Loading -> null
                    }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val watchProgress = combine(
        selectedEpisode,
        userSessionDataStore.currentUserId.filterNotNull()
    ) { episode, userId ->
        episode to userId
    }.flatMapLatest { (episode, userId) ->
        watchProgressRepository
            .getAsFlow(
                id = media.id,
                type = media.type,
                ownerId = userId,
            ).filterNotNull()
            .map {
                val progress = it.watchData
                if (progress is EpisodeProgress) {
                    val isSameEpisode = progress.isSameEpisode(
                        otherEpisode = episode?.number ?: -1,
                        otherSeason = episode?.season ?: -1,
                        otherMediaId = media.id,
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
        if (isLocalPlayback || changeServerJob?.isActive == true) return

        changeServerJob = viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val episode = _uiState.value.currentEpisode
            val currentProvider = _uiState.value.currentProvider.ifEmpty {
                _playerErrors.emit(UiText.from(R.string.error_no_provider_selected))
                return@launch
            }

            val cache = mediaLinksRepository.getLinksByProvider(
                ownerId = userId,
                mediaId = media.id,
                providerId = currentProvider,
                seasonNumber = episode?.season,
                episodeNumber = episode?.number
            )

            if (cache == null) {
                _playerErrors.emit(UiText.from(R.string.error_on_server_change_fail))
                return@launch
            }

            if (serverIndex !in cache.streams.indices) return@launch

            _uiState.update { it.copy(currentServer = serverIndex) }
            withContext(appDispatchers.main) {
                player.prepare(
                    cache = cache,
                    startPositionMs = player.currentPosition,
                )
            }
        }
    }

    fun onProviderChange(providerId: String) {
        if (isLocalPlayback || changeProviderJob?.isActive == true) return

        queueNextEpisodeJob?.cancel()
        changeEpisodeJob?.cancel()

        updateWatchProgress()

        val currentServer = _uiState.value.currentServer
        val currentProvider = _uiState.value.currentProvider
        _uiState.update {
            it.copy(currentProvider = providerId, currentServer = -1)
        }

        changeProviderJob = viewModelScope.launch {
            val cache = loadLinks(
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
        if (isLocalPlayback) return

        val state = _uiState.value.loadLinksState
        if (state !is LoadLinksState.Extracting && state !is LoadLinksState.Success) return

        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val episode = _uiState.value.currentEpisode
            val currentProvider = _uiState.value.currentProvider.ifEmpty {
                _playerErrors.emit(UiText.from(R.string.error_no_provider_selected))
                return@launch
            }

            val cache = mediaLinksRepository.getLinksByProvider(
                ownerId = userId,
                mediaId = media.id,
                providerId = currentProvider,
                episodeNumber = episode?.number,
                seasonNumber = episode?.season
            )

            if (cache == null || !cache.hasValidLinks) return@launch

            _uiState.update {
                it.copy(
                    currentProvider = cache.providerId,
                    loadLinksState = LoadLinksState.Idle,
                )
            }

            withContext(appDispatchers.main) {
                player.prepare(
                    cache = cache,
                    startPositionMs = player.currentPosition,
                )
            }
        }
    }

    fun onServerFail(server: String) {
        if (isLocalPlayback) return

        appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            mediaLinksRepository.setLinkStatus(server, ownerId = userId, isDead = true)
        }
    }

    fun onCancelLoading() {
        changeProviderJob?.cancel()
        changeServerJob?.cancel()
        changeEpisodeJob?.cancel()
        queueNextEpisodeJob?.cancel()
        _uiState.update { it.copy(loadLinksState = LoadLinksState.Idle) }
    }

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

            val cache = loadLinks(
                providerId = _uiState.value.currentProvider,
                episode = episode,
            )

            if (cache != null) {
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
    ): MediaLinksWithData? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        val cache = mediaLinksRepository.getLinksByProvider(
            ownerId = userId,
            mediaId = media.id,
            providerId = providerId,
            episodeNumber = episode?.number,
            seasonNumber = episode?.season
        )

        if (cache != null && cache.hasValidLinks) {
            return cache
        }

        val response = getMediaLinks(
            media = media,
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

        return mediaLinksRepository.getLinksByProvider(
            ownerId = userId,
            mediaId = media.id,
            providerId = providerId,
            episodeNumber = episode?.number,
            seasonNumber = episode?.season
        )
    }

    @MainThread
    private fun AppPlayer.prepare(
        cache: MediaLinksWithData,
        startPositionMs: Long,
        preferredServer: String? = null,
    ) {
        val servers = cache.streams
        val subtitles = cache.subtitles.toPlayerSubtitles()

        val prefs = playerPreferences.value

        val currentServer = when {
            preferredServer != null -> {
                servers.indexOfFirst { it.url == preferredServer }
            }

            !prefs.isAutoSelectingServer -> {
                0
            }

            prefs.isAutoSelectingServer && _uiState.value.currentServer !in servers.indices -> {
                servers.getIndexOfPreferredQuality(prefs.quality) {
                    (containsMatchIn(it.label) || containsMatchIn(it.url)) &&
                        it.isValid &&
                        !it.isThirdPartyGateway
                }
            }

            else -> {
                _uiState.value.currentServer
            }
        }

        if (currentServer !in servers.indices) {
            _playerErrors.tryEmit(UiText.from(R.string.error_no_valid_servers_found))
            return
        }

        _uiState.update { it.copy(currentServer = currentServer) }

        prepare(
            server = servers[currentServer].toPlayerServer(),
            subtitles = subtitles,
            startPositionMs = startPositionMs,
        )

        updateWatchProgress()
    }

    private suspend fun getNextEpisode(episode: Episode?): Episode? {
        // A local download has no provider to resolve the show's episode list from — its
        // `media` is a PartialMedia, never a real Show. Offline next-episode support is a
        // separate feature; see MediaDownloadRepository.getBatch/observeByMedia.
        if (episode == null || isLocalPlayback) return null

        return getNextEpisode(
            show = media as Show,
            season = episode.season,
            episode = episode.number,
        )
    }

    private fun getDefaultWatchProgress(): WatchProgress {
        val userId = runBlocking { userSessionDataStore.currentUserId.filterNotNull().first() }

        // A class check (`when (media) { is Movie -> ...`) would throw for a PartialMedia — the
        // type a locally-resolved download's media always is. Switch on the type enum instead,
        // which every MediaMetadata implementation reports correctly regardless of its class.
        return when (media.type) {
            MediaType.MOVIE -> MovieProgress(
                mediaId = media.id,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
            )

            MediaType.SHOW -> EpisodeProgress(
                mediaId = media.id,
                ownerId = userId,
                progress = 0L,
                status = WatchStatus.WATCHING,
                seasonNumber = selectedEpisode.value!!.season,
                episodeNumber = selectedEpisode.value!!.number,
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun AppPlayer.observePlaybackProgress() {
        viewModelScope.launch(appDispatchers.main) {
            launch {
                listenTo(Player.EVENT_PLAYBACK_STATE_CHANGED) { events ->
                    if (!events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                        return@listenTo
                    }

                    val isFinished = !isPlaying && currentPosition >= duration && duration > 0
                    val nextEpisode = _uiState.value.nextEpisode
                    if (isFinished && nextEpisode != null) {
                        onEpisodeChange(nextEpisode)
                        return@listenTo
                    }
                }
            }

            launch {
                val playerPrefs = dataStoreManager
                    .getUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)

                listenTo(Player.EVENT_IS_PLAYING_CHANGED) { events ->
                    if (!events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                        return@listenTo
                    }

                    updateWatchProgress()

                    autoQueueNextEpisodeJob?.cancel()
                    autoQueueNextEpisodeJob = launch {
                        while (isPlaying) {
                            if (duration <= 0) continue

                            val isQueueingNextEpisode =
                                currentPosition >= (duration * playerPrefs.thresholdForNextEpisodeQueue)
                            if (media is Show && isQueueingNextEpisode) {
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

            if (currentPosition > 60_000L) {
                setWatchProgress(
                    media = media,
                    watchProgress = progress,
                )
            }

            delay(1500L.milliseconds)

            val isPlaying = withContext(appDispatchers.main) {
                player.isPlaying
            }

            syncToScrobblers(
                action = if (isPlaying) ScrobbleAction.START else ScrobbleAction.STOP,
                media = media,
                episode = selectedEpisode.value,
                watchProgress = progress,
            ).collect { response ->
                when (response) {
                    is Async.Failure -> _playerErrors.emit(response.message)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun getSavedStartPositionMs(episode: Episode? = null): Long {
        val userId = ownerId.filterNotNull().first()
        val watchProgress = if (episode == null) {
            watchProgressRepository
                .get(
                    id = media.id,
                    type = media.type,
                    ownerId = userId,
                )?.watchData
        } else {
            watchProgressRepository.getEpisodeProgress(
                tvShowId = media.id,
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
        if (isLocalPlayback) {
            initializeLocalPlayback()
            return
        }

        viewModelScope.launch {
            launch {
                userSessionDataStore.currentUserId
                    .filterNotNull()
                    .flatMapLatest { userId ->
                        providerRepository
                            .getProvidersWithCapabilityAsFlow(
                                ownerId = userId,
                                capability = ProviderCapability.MEDIA_LINK
                            ).mapLatest { list ->
                                var foundMetadataProvider = false
                                val mappedList = list.fastMapNotNull { provider ->
                                    if (provider.id == media.providerId) {
                                        foundMetadataProvider = true
                                        return@fastMapNotNull provider.metadata
                                    }

                                    if (!provider.isMediaLinkEnabled) return@fastMapNotNull null

                                    provider.metadata
                                }

                                if (!foundMetadataProvider) {
                                    val metadata = getProviderMetadata(
                                        id = media.providerId
                                    ) ?: return@mapLatest emptyList() // Fails player and navigate back

                                    return@mapLatest mappedList + listOf(metadata)
                                }

                                mappedList
                            }
                    }.catch { error ->
                        errorLog(error)
                        _providers.emit(Async.Success(emptyList()))
                    }.collectLatest {
                        _providers.emit(Async.Success(it))
                    }
            }

            launch {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()

                val cache = mediaLinksRepository.getLinksByProvider(
                    ownerId = userId,
                    mediaId = media.id,
                    providerId = media.providerId,
                    episodeNumber = selectedEpisode.value?.number,
                    seasonNumber = selectedEpisode.value?.season
                )

                if (cache == null || cache.streams.isEmpty()) {
                    _playerErrors.emit(UiText.from(R.string.error_no_valid_servers_found))
                    return@launch
                }

                _servers.update { Async.Success(cache.streams.toPlayerServers()) }

                val nextEpisode = getNextEpisode(initialEpisode)
                _uiState.update { it.copy(nextEpisode = nextEpisode) }

                withContext(appDispatchers.main) {
                    player.prepare(
                        cache = cache,
                        startPositionMs = getSavedStartPositionMs(initialEpisode),
                        preferredServer = providerRequest?.preferredStreamUrl
                    )
                }
            }

            launch {
                combine(
                    userSessionDataStore.currentUserId.filterNotNull(),
                    selectedEpisode.debounce(600.milliseconds),
                    _uiState
                        .mapNotNull { state -> state.currentProvider.takeIf { it.isNotEmpty() } }
                        .distinctUntilChanged()
                ) { userId, episode, providerId ->
                    Triple(userId, episode, providerId)
                }.flatMapLatest { (userId, episode, providerId) ->
                    mediaLinksRepository
                        .observeLinksByProvider(
                            ownerId = userId,
                            mediaId = media.id,
                            providerId = providerId,
                            episodeNumber = episode?.number,
                            seasonNumber = episode?.season
                        ).mapLatest {
                            it?.streams?.toPlayerServers() ?: emptyList()
                        }.catch { error ->
                            errorLog(error)
                            emit(emptyList())
                        }
                }.collectLatest {
                    _servers.emit(Async.Success(it))
                }
            }
        }
    }

    /** Local playback never touches [mediaLinksRepository]'s link-cache methods,
     * [providerRepository], or [getProviderMetadata] — the file and its metadata are already
     * fully resolved in [localPlayback]. There is exactly one "server" (the file itself) and no
     * providers, mirroring that in [_servers]/[_providers]. */
    private fun initializeLocalPlayback() {
        val playback = requireNotNull(localPlayback)

        viewModelScope.launch {
            _providers.update { Async.Success(emptyList()) }

            val file = playback.file
            if (file == null) {
                _playerErrors.emit(UiText.from(R.string.error_no_valid_servers_found))
                return@launch
            }

            val server = file.toPlayerServer(label = playback.item.mediaTitle)
            val subtitles = file.toPlayerSubtitles()

            _servers.update { Async.Success(listOf(server)) }
            _uiState.update { it.copy(currentServer = 0) }

            withContext(appDispatchers.main) {
                player.prepare(
                    server = server,
                    subtitles = subtitles,
                    startPositionMs = getSavedStartPositionMs(playback.episode),
                )
            }

            updateWatchProgress()
        }
    }
}

@Immutable
internal data class PlayerUiState(
    val currentProvider: String = "",
    val currentSeason: Int? = null,
    val currentEpisode: Episode? = null,
    val currentServer: Int = -1,
    val nextEpisode: Episode? = null,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
