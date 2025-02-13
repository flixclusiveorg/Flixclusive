package com.flixclusive.core.ui.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.data.provider.ProviderRepository
import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinks
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.domain.library.recent.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.toWatchHistoryItem
import com.flixclusive.model.database.util.getSavedTimeForFilm
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.datastore.user.player.PlayerQuality.Companion.getIndexOfPreferredQuality
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import kotlin.math.max
import com.flixclusive.core.locale.R as LocaleR

/**
 * TODO: Remove this bitch ass ugly ass code
 * */
abstract class BasePlayerViewModel(
    args: PlayerScreenNavArgs,
    client: OkHttpClient,
    context: Context,
    playerCacheManager: PlayerCacheManager,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val providerRepository: ProviderRepository,
    private val dataStoreManager: DataStoreManager,
    private val seasonProviderUseCase: SeasonProviderUseCase,
    private val getMediaLinksUseCase: GetMediaLinksUseCase,
    private val watchTimeUpdaterUseCase: WatchTimeUpdaterUseCase,
    private val userSessionManager: UserSessionManager,
    private val cachedLinksRepository: CachedLinksRepository
) : ViewModel() {
    val film = args.film

    val player =
        FlixclusivePlayerManager(
            client = client,
            context = context,
            playerCacheManager = playerCacheManager,
            dataStoreManager = dataStoreManager,
            showErrorCallback = {
                showErrorSnackbar(
                    message = it,
                    isInternalPlayerError = true,
                )
            },
        )

    val providers by lazy { providerRepository.getEnabledProviders() }

    private val _dialogState = MutableStateFlow<MediaLinkResourceState>(MediaLinkResourceState.Idle)
    val dialogState: StateFlow<MediaLinkResourceState> = _dialogState.asStateFlow()

    private val _season = MutableStateFlow<Resource<Season?>>(Resource.Loading)
    val season = _season.asStateFlow()

    var areControlsVisible by mutableStateOf(true)
    var areControlsLocked by mutableStateOf(false)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentSelectedEpisode = MutableStateFlow(args.episodeToPlay)
    open val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    val cachedLinksAsFlow = _currentSelectedEpisode
        .flatMapLatest { episodeToPlay ->
            val cacheKey = CacheKey.createFilmOnlyKey(film.identifier, episodeToPlay)
            cachedLinksRepository.observeCache(cacheKey)
        }
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CachedLinks(),
        )

    protected val cachedLinks: CachedLinks get() {
        val key = CacheKey.createFilmOnlyKey(film.identifier, _currentSelectedEpisode.value)
        return cachedLinksRepository.getCache(key) ?: CachedLinks()
    }

    /**
     * For the next episode to
     * seamlessly watch tv shows
     * */
    private var nextEpisodeToUse: Episode? by mutableStateOf(null)
    private var isNextEpisodeLoaded = false
    var isLastEpisode by mutableStateOf(film.filmType == FilmType.MOVIE)
        private set

    private val userId: Int?
        get() = userSessionManager.currentUser.value?.id

    val watchHistoryItem =
        userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                watchHistoryRepository
                    .getWatchHistoryItemByIdInFlow(
                        itemId = film.identifier,
                        ownerId = user.id,
                    )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    runBlocking {
                        userId?.let {
                            watchHistoryRepository.getWatchHistoryItemById(film.identifier, it)
                                ?: film.toWatchHistoryItem(ownerId = it)
                        }
                    },
            )

    val playerPreferences =
        dataStoreManager
            .getUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)
            .asStateFlow(viewModelScope)

    val subtitlesPreferences =
        dataStoreManager
            .getUserPrefs<SubtitlesPreferences>(UserPreferences.SUBTITLES_PREFS_KEY)
            .asStateFlow(viewModelScope)

    private var loadLinksFromNewProviderJob: Job? = null
    private var loadLinksJob: Job? = null
    private var loadNextLinksJob: Job? = null
    private var onSeasonChangeJob: Job? = null

    open fun resetUiState() {
        _uiState.update {
            val preferredQuality = playerPreferences.value.quality
            val preferredResizeMode = playerPreferences.value.resizeMode
            val indexOfPreferredServer =
                cachedLinks.streams
                    .getIndexOfPreferredQuality(preferredQuality = preferredQuality)

            it.copy(
                selectedSourceLink = indexOfPreferredServer,
                selectedResizeMode = preferredResizeMode,
                selectedProvider = cachedLinks.providerId,
            )
        }
    }

    /**
     *
     * Obtains the saved time for the current [CachedLinks] to be watched.
     *
     * @param episode an optional parameter for episode to be watched if the film is a [TvShow]
     *
     * @return A pair of time in [Long] format - Pair(watchTime, durationTime)
     * */
    fun getSavedTimeForSourceData(episode: Episode? = null): Pair<Long, Long> {
        val episodeToUse = episode ?: currentSelectedEpisode.value

        val watchHistoryItemToUse = watchHistoryItem.value ?: return 0L to 0L

        isLastEpisode =
            checkIfLastEpisode(
                episodeToCheck = episodeToUse,
                watchHistoryItem = watchHistoryItemToUse,
            )

        return getSavedTimeForFilm(
            watchHistoryItemToUse,
            episodeToUse,
        )
    }

    private fun checkIfLastEpisode(
        episodeToCheck: Episode?,
        watchHistoryItem: WatchHistoryItem?,
    ): Boolean {
        val episode = episodeToCheck ?: return true
        val lastSeason = watchHistoryItem?.seasons
        val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

        return episode.season == lastSeason && episode.number == lastEpisode
    }

    /**
     *
     * A callback to show an error message to the player screen.
     *
     * @param message the global [UiText] message to be sent and parse by the player
     * */
    protected abstract fun showErrorSnackbar(
        message: UiText,
        isInternalPlayerError: Boolean = false,
    )

    fun onAddSubtitle(subtitle: Subtitle) {
        val providerId = cachedLinks.providerId
        val key = CacheKey.create(
            filmId = film.identifier,
            providerId = providerId,
            episode = _currentSelectedEpisode.value
        )

        cachedLinksRepository.addSubtitle(key, subtitle)
    }

    fun onSeasonChange(seasonNumber: Int) {
        if (onSeasonChangeJob?.isActive == true) {
            return
        }

        onSeasonChangeJob =
            viewModelScope.launch {
                if (!film.isFromTmdb) {
                    val tvShow = film as TvShow
                    val season =
                        tvShow.seasons
                            .find { it.number == seasonNumber }

                    if (season != null) {
                        _season.value = Resource.Success(season)
                    }

                    return@launch
                }

                seasonProviderUseCase
                    .asFlow(
                        tvShow = film as TvShow,
                        seasonNumber = seasonNumber,
                    ).collectLatest { result ->
                        if (result is Resource.Success) {
                            val watchHistoryItem =
                                watchHistoryRepository.getWatchHistoryItemById(
                                    itemId = film.identifier,
                                    ownerId = userId ?: return@collectLatest,
                                )

                            watchHistoryItem?.let { item ->
                                result.data?.episodes?.size?.let {
                                    val newEpisodesMap = item.episodes.toMutableMap()
                                    newEpisodesMap[seasonNumber] = it

                                    watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
                                }
                            }
                        }

                        _season.value = result
                    }
            }
    }

    fun onProviderChange(newProvider: String) {
        if (loadLinksFromNewProviderJob?.isActive == true) {
            return
        }

        // Cancel episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        updateWatchHistory(
            currentTime = player.currentPosition,
            duration = player.duration,
        )

        loadLinksFromNewProviderJob =
            viewModelScope.launch {
                val oldSelectedSource = _uiState.value.selectedProvider
                updateProviderSelected(newProvider)

                getMediaLinksUseCase(
                    film = film as FilmMetadata,
                    preferredProvider = newProvider,
                    watchHistoryItem = watchHistoryItem.value,
                    episode = currentSelectedEpisode.value,
                    onSuccess = { _ ->
                        resetUiState()
                        resetNextEpisodeQueue()
                    },
                    onError = {
                        updateProviderSelected(oldSelectedSource)
                        showErrorSnackbar(it)
                    },
                ).collect { state ->
                    when {
                        state.isIdle ||
                            state.isError ||
                            state.isSuccess ||
                            state.isSuccessWithTrustedProviders -> {
                            _uiState.update {
                                it.copy(selectedProviderState = PlayerProviderState.SELECTED)
                            }
                        }

                        state.isLoading -> {
                            _uiState.update {
                                it.copy(selectedProviderState = PlayerProviderState.LOADING)
                            }
                        }
                    }
                }
            }
    }

    fun onResizeModeChange(resizeMode: Int) {
        _uiState.update { it.copy(selectedResizeMode = resizeMode) }
    }

    fun onPanelChange(opened: Int) {
        _uiState.update { it.copy(lastOpenedPanel = opened) }
    }

    fun onConsumePlayerDialog() {
        _dialogState.update { MediaLinkResourceState.Idle }
    }

    fun onServerChange(index: Int? = null) {
        val preferredQuality = playerPreferences.value.quality
        val indexToUse =
            index ?: cachedLinks.streams.getIndexOfPreferredQuality(
                preferredQuality = preferredQuality,
            )

        updateWatchHistory(
            currentTime = player.currentPosition,
            duration = player.duration,
        )

        _uiState.update {
            it.copy(selectedSourceLink = max(indexToUse, 0))
        }
    }

    fun toggleVideoTimeReverse() {
        viewModelScope.launch {
            dataStoreManager.updateUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY) {
                it.copy(isDurationReversed = !it.isDurationReversed)
            }
        }
    }

    private fun updateProviderSelected(source: String?) {
        _uiState.update {
            it.copy(selectedProvider = source)
        }
    }

    fun updateWatchHistory(
        currentTime: Long,
        duration: Long,
    ) {
        viewModelScope.launch {
            watchTimeUpdaterUseCase(
                watchHistoryItem =
                    watchHistoryItem.value
                        ?: film.toWatchHistoryItem(
                            ownerId = userId ?: return@launch,
                        ),
                currentTime = currentTime,
                totalDuration = duration,
                currentSelectedEpisode = currentSelectedEpisode.value,
            )
        }
    }

    /**
     *
     * Obtains the queried [Season] data with the given parameter [seasonNumber]
     * from local state or remote fetch.
     *
     * See: [fetchSeasonFromMetaProvider]
     *
     * @param seasonNumber the season to be queried.
     *
     * */
    private suspend fun fetchSeasonIfNeeded(seasonNumber: Int): Resource<Season?> {
        var currentLoadedSeasonNumber = _season.value
        if (currentLoadedSeasonNumber.data?.number != seasonNumber) {
            val result = fetchSeasonFromMetaProvider(seasonNumber = seasonNumber)

            _season.value = result
            currentLoadedSeasonNumber = result
        }

        return currentLoadedSeasonNumber
    }

    /**
     *
     * Obtains the next episode based on the given [Episode].
     * It returns null if an error has occured or it can't find the episode to be queried.
     *
     * @param onError an optional callback if caller wants to call [showErrorSnackbar]
     *
     * */
    private suspend fun Episode.getNextEpisode(onError: ((UiText) -> Unit)? = null): Episode? {
        val nextEpisode: Episode?

        val episodeSeasonNumber = this.season
        val episodeNumber = this.number

        val seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        if (seasonToUse is Resource.Failure) {
            onError?.invoke(seasonToUse.error!!)
            return null
        }

        val episodesList = seasonToUse.data!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if (episodesList.last().number == nextEpisodeNumberToWatch) {
            return episodesList.last()
        }

        val isThereANextEpisode = episodesList.last().number > nextEpisodeNumberToWatch
        val isThereANextSeason = seasonToUse.data!!.number + 1 <= (film as TvShow).totalSeasons

        nextEpisode =
            if (isThereANextEpisode) {
                episodesList.firstOrNull {
                    it.number == nextEpisodeNumberToWatch
                }
            } else if (isThereANextSeason) {
                fetchSeasonIfNeeded(seasonNumber = episodeSeasonNumber + 1).run {
                    if (this is Resource.Failure) {
                        onError?.invoke(error!!)
                    }

                    data?.episodes?.firstOrNull()
                }
            } else {
                null
            }

        if (nextEpisode == null && !isThereANextSeason) {
            onError?.invoke(
                UiText.StringResource(
                    LocaleR.string.episode_non_existent_error_message_format,
                    nextEpisodeNumberToWatch,
                    seasonToUse.data!!.number,
                ),
            )
        }

        return nextEpisode
    }

    /**
     * Callback function triggered when an episode is clicked.
     *
     * @param episodeToWatch The next episode to be played, or null if not available.
     */
    fun onEpisodeClick(episodeToWatch: Episode? = null) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorSnackbar(UiText.StringResource(LocaleR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        viewModelScope.launch {
            // If there's a queued next episode, skip loading the video and go next
            val isLoadingNextEpisode = episodeToWatch == null
            val hasNextEpisode = getQueuedEpisode()

            if (isLoadingNextEpisode && hasNextEpisode) {
                return@launch
            }

            var episode = episodeToWatch
            if (isLoadingNextEpisode) {
                episode =
                    currentSelectedEpisode.value!!
                        .getNextEpisode(
                            onError = { showErrorSnackbar(it) },
                        ).also {
                            if (it == null) {
                                return@launch
                            }
                        }
            }

            loadMediaLinks(episode)
        }
    }

    /**
     * Function to load [CachedLinks] from a provider
     *
     * @param episodeToWatch an optional parameter for the episode to watch if film to be watched is a [TvShow]
     */
    fun loadMediaLinks(episodeToWatch: Episode? = null) {
        if (loadLinksFromNewProviderJob?.isActive == true || loadLinksJob?.isActive == true) {
            showErrorSnackbar(UiText.StringResource(LocaleR.string.load_link_job_active_error_message))
            return
        }

        // Cancel next episode queueing job
        if (loadNextLinksJob?.isActive == true) {
            loadNextLinksJob?.cancel()
            loadNextLinksJob = null
        }

        loadLinksJob =
            viewModelScope.launch {
                getMediaLinksUseCase(
                    film = film as FilmMetadata,
                    watchId = cachedLinks.watchId,
                    preferredProvider = _uiState.value.selectedProvider,
                    watchHistoryItem = watchHistoryItem.value,
                    episode = episodeToWatch,
                    onSuccess = { newEpisode ->
                        _currentSelectedEpisode.value = newEpisode

                        resetUiState()
                        resetNextEpisodeQueue()
                    },
                ).collect {
                    _dialogState.value = it
                }
            }
    }

    /**
     *
     * Obtains the next episode queued by the player.
     *
     * @return [Boolean] true if success, otherwise false.
     * */
    private suspend fun getQueuedEpisode(): Boolean {
        if (
            isNextEpisodeLoaded &&
            nextEpisodeToUse != null
        ) {
            _season.value = fetchSeasonFromMetaProvider(nextEpisodeToUse!!.season)
            _currentSelectedEpisode.value = nextEpisodeToUse

            resetUiState()
            resetNextEpisodeQueue()

            return true
        }

        return false
    }

    /**
     * Reset the episode queue
     * */
    private fun resetNextEpisodeQueue() {
        nextEpisodeToUse = null
        isNextEpisodeLoaded = false
    }

    /**
     *
     * Callback function to silently queue up the next episode
     */
    fun onQueueNextEpisode() {
        if (loadNextLinksJob?.isActive == true ||
            isNextEpisodeLoaded ||
            loadLinksJob?.isActive == true ||
            loadLinksFromNewProviderJob?.isActive == true
        ) {
            return
        }

        loadNextLinksJob =
            viewModelScope.launch {
                val episode =
                    currentSelectedEpisode.value!!
                        .getNextEpisode()

                getMediaLinksUseCase(
                    film = film as FilmMetadata,
                    watchId = cachedLinks.watchId,
                    preferredProvider = _uiState.value.selectedProvider,
                    watchHistoryItem = watchHistoryItem.value,
                    episode = episode,
                    onSuccess = { newEpisode ->
                        nextEpisodeToUse = newEpisode
                        isNextEpisodeLoaded = true
                    },
                ).collect()
            }
    }

    /**
     *
     * Fetches the [Season] data from the main meta provider
     * then if it successfully obtains the data, save it to
     * the watch history episodes map for local use.
     *
     * @param seasonNumber the season to be queried.
     *
     * */
    protected suspend fun fetchSeasonFromMetaProvider(seasonNumber: Int): Resource<Season> {
        val result =
            seasonProviderUseCase(
                tvShow = film as TvShow,
                seasonNumber = seasonNumber,
            )

        watchHistoryItem.value?.let { item ->
            result.data?.episodes?.size?.let {
                val newEpisodesMap = item.episodes.toMutableMap()
                newEpisodesMap[seasonNumber] = it
                watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
            }
        }

        return result
    }
}
