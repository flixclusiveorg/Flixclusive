package com.flixclusive.presentation.common.viewmodels.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.SourceData
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.entities.toWatchHistoryItem
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.SourceLinksProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.utils.WatchHistoryUtils
import com.flixclusive.presentation.common.player.FlixclusivePlayer
import com.flixclusive.presentation.common.player.PlayerUiState
import com.flixclusive.presentation.navArgs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max

abstract class BasePlayerViewModel(
    @ApplicationContext context: Context,
    watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val appSettingsManager: AppSettingsManager,
    private val sourceLinksProvider: SourceLinksProviderUseCase,
) : ViewModel() {
    private val args = savedStateHandle.navArgs<PlayerScreenNavArgs>()

    val film = args.film

    val sourceData: SourceData
        get() = sourceLinksProvider.getLinks(
            filmId = film.id,
            episode = _currentSelectedEpisode.value
        )

    val sourceProviders = sourceLinksProvider.providers
    protected val seasonCount: Int?
        get() = (film as? TvShow)?.totalSeasons

    private val _dialogState = MutableStateFlow<SourceDataState>(SourceDataState.Idle)
    val dialogState: StateFlow<SourceDataState> = _dialogState.asStateFlow()

    var areControlsVisible by mutableStateOf(true)
    var areControlsLocked by mutableStateOf(false)
    var isLastEpisode by mutableStateOf(false)
        private set
    private var isNextEpisodeLoaded = false

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentSelectedEpisode = MutableStateFlow(args.episodeToPlay)
    open val currentSelectedEpisode = _currentSelectedEpisode.asStateFlow()

    /**
     * For the next episode to
     * seamlessly watch tv shows
     * */
    private var nextEpisodeToUse: TMDBEpisode? by mutableStateOf(null)

    val watchHistoryItem = watchHistoryRepository
        .getWatchHistoryItemByIdInFlow(film.id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { watchHistoryRepository.getWatchHistoryItemById(film.id) ?: film.toWatchHistoryItem() }
        )

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    val player = FlixclusivePlayer(
        context = context,
        appSettings = appSettings.value
    )

    private var loadLinksJob: Job? = null

    override fun onCleared() {
        sourceLinksProvider.clearCache()
        super.onCleared()
    }

    open fun resetUiState() {
        _uiState.update {
            val preferredServer = appSettings.value.preferredQuality
            val indexOfPreferredServer = sourceData.cachedLinks.indexOfFirst { server ->
                server.name.contains(preferredServer, true)
                || server.name == preferredServer
            }

            it.copy(
                selectedSourceLink = max(indexOfPreferredServer, 0),
                selectedProvider = sourceData.sourceNameUsed,
            )
        }
    }

    // =======================
    fun getSavedTimeForSourceData(episode: TMDBEpisode? = null): Pair<Long, Long> {
        val episodeToUse = episode ?: currentSelectedEpisode.value

        val watchHistoryItemToUse = watchHistoryItem.value ?: WatchHistoryItem()

        isLastEpisode = checkIfLastEpisode(episodeToUse, watchHistoryItemToUse)
        return WatchHistoryUtils.getSavedTimeForFilm(
            watchHistoryItemToUse,
            episodeToUse
        )
    }

    private fun checkIfLastEpisode(
        episodeToCheck: TMDBEpisode?,
        watchHistoryItem: WatchHistoryItem?,
    ): Boolean {
        val episode = episodeToCheck ?: return false
        val lastSeason = watchHistoryItem?.seasons
        val lastEpisode = watchHistoryItem?.episodes?.get(lastSeason)

        return episode.season == lastSeason && episode.episode == lastEpisode
    }

    protected abstract fun onErrorCallback(message: String? = null)

    fun onProviderChange(newSource: String) {
        if (loadLinksJob?.isActive == true)
            return

        loadLinksJob = viewModelScope.launch {
            val oldSelectedSource = _uiState.value.selectedProvider
            updateSourceSelected(newSource)

            sourceLinksProvider.loadLinks(
                film = film,
                preferredProviderName = newSource,
                isChangingProvider = true,
                watchHistoryItem = watchHistoryItem.value,
                episode = currentSelectedEpisode.value,
                onSuccess = { _ -> 
                    resetUiState()
                },
                onError = {
                    updateSourceSelected(oldSelectedSource)
                    onErrorCallback("Failed to retrieve provider [$newSource]")
                }
            ).collect { state ->
                when (state) {
                    SourceDataState.Idle,
                    is SourceDataState.Error,
                    is SourceDataState.Unavailable,
                    SourceDataState.Success -> _uiState.update {
                        it.copy(selectedProviderState = Resource.Success(null))
                    }

                    is SourceDataState.Extracting, is SourceDataState.Fetching -> _uiState.update {
                        it.copy(selectedProviderState = Resource.Loading)
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
        _dialogState.update { SourceDataState.Idle }
    }

    fun updateVolume(strength: Float) {
        _uiState.update {
            it.copy(volume = strength)
        }
    }

    fun updateScreenBrightness(strength: Float) {
        _uiState.update {
            it.copy(screenBrightness = strength)
        }
    }

    fun onServerChange(index: Int? = null) {
        val indexToUse = index ?: sourceData.cachedLinks.indexOfFirst {
            it.name.contains(appSettings.value.preferredQuality, true)
            || it.name == appSettings.value.preferredQuality
        }

        _uiState.update {
            it.copy(selectedSourceLink = max(indexToUse, 0))
        }
    }

    fun toggleVideoTimeReverse() {
        viewModelScope.launch {
            appSettingsManager.updateData(
                appSettings.value.run {
                    copy(isPlayerTimeReversed = !isPlayerTimeReversed)
                }
            )
        }
    }

    private fun updateSourceSelected(source: String?) {
        _uiState.update {
            it.copy(selectedProvider = source)
        }
    }

    fun updateWatchHistory(
        currentTime: Long,
        duration: Long,
    ) {
        viewModelScope.launch {
            watchHistoryItem.value?.let {
                watchHistoryItemManager.updateWatchHistoryItem(
                    watchHistoryItem = it,
                    currentTime = currentTime,
                    totalDuration = duration,
                    currentSelectedEpisode = currentSelectedEpisode.value
                )
            }
        }
    }

    protected abstract suspend fun TMDBEpisode.fetchSeasonIfNeeded(seasonNumber: Int): Season?

    private suspend fun TMDBEpisode.getNextEpisode(
        seasonCount: Int,
        updateSeason: ((Season?) -> Unit)? = null,
    ): TMDBEpisode {
        val nextEpisode: TMDBEpisode

        val episodeSeasonNumber = this.season
        val episodeNumber = this.episode

        var seasonToUse = fetchSeasonIfNeeded(episodeSeasonNumber)

        val episodesList = seasonToUse!!.episodes
        val nextEpisodeNumberToWatch = episodeNumber + 1

        if (episodesList.last().episode == nextEpisodeNumberToWatch)
            return episodesList.last()

        nextEpisode = if (episodesList.last().episode > nextEpisodeNumberToWatch) {
            episodesList.find {
                it.episode == nextEpisodeNumberToWatch
            }
                ?: throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")
        } else if (seasonToUse.seasonNumber + 1 <= seasonCount) {
            seasonToUse =
                fetchSeasonIfNeeded(seasonNumber = currentSelectedEpisode.value!!.season + 1)

            updateSeason?.invoke(seasonToUse)

            seasonToUse!!.episodes.first()
        } else throw NullPointerException("Episode $nextEpisodeNumberToWatch doesn't exist on Season ${seasonToUse.seasonNumber}")

        return nextEpisode
    }

    /**
     * Function to load [SourceData] from a provider
     *
     */
    fun loadSourceData(
        episodeToWatch: TMDBEpisode? = null,
        updateSeason: ((Season?) -> Unit)? = null,
    ) {
        if (loadLinksJob?.isActive == true)
            return

        loadLinksJob = viewModelScope.launch {
            // If there's a queued next episode, stop the video and go next
            val isLoadingNextEpisode = episodeToWatch == null && film.filmType == FilmType.TV_SHOW
            val hasNextEpisode = getQueuedEpisode(
                onSuccess = { newEpisode ->
                    _currentSelectedEpisode.value = newEpisode
                },
                updateSeason = updateSeason
            )

            if (isLoadingNextEpisode && hasNextEpisode) {
                return@launch
            }

            var episode = episodeToWatch
            if (episode == null) {
                episode = currentSelectedEpisode.value!!.getNextEpisode(
                    seasonCount = seasonCount!!,
                    updateSeason = updateSeason
                )
            }

            sourceLinksProvider.loadLinks(
                film = film,
                mediaId = sourceData.mediaId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                onSuccess = { newEpisode ->
                    _currentSelectedEpisode.value = newEpisode

                    resetUiState()
                    resetNextEpisodeQueue()
                }
            ).collect { state ->
                _dialogState.update { state }
            }
        }
    }

    private suspend fun getQueuedEpisode(
        onSuccess: (TMDBEpisode?) -> Unit,
        updateSeason: ((Season?) -> Unit)? = null,
    ): Boolean {
        if (
            isNextEpisodeLoaded
            && nextEpisodeToUse != null
        ) {
            currentSelectedEpisode.value!!.getNextEpisode(
                seasonCount = seasonCount!!,
                updateSeason = updateSeason
            )
            onSuccess(nextEpisodeToUse)
            resetUiState()

            resetNextEpisodeQueue()

            return true
        }

        return false
    }

    // Reset the episode queue
    private fun resetNextEpisodeQueue() {
        nextEpisodeToUse = null
        isNextEpisodeLoaded = false
    }

    /**
     * Callback function to queue up next episode
     *
     */
    fun onQueueNextEpisode() {
        if (loadLinksJob?.isActive == true || isNextEpisodeLoaded)
            return

        loadLinksJob = viewModelScope.launch {
            val episode = currentSelectedEpisode.value!!
                .getNextEpisode(seasonCount = seasonCount!!)

            sourceLinksProvider.loadLinks(
                film = film,
                mediaId = sourceData.mediaId,
                preferredProviderName = _uiState.value.selectedProvider,
                watchHistoryItem = watchHistoryItem.value,
                episode = episode,
                onSuccess = { newEpisode ->
                    nextEpisodeToUse = newEpisode
                    isNextEpisodeLoaded = true
                }
            ).collect()
        }
    }

    protected abstract suspend fun fetchSeasonFromProvider(showId: Int, seasonNumber: Int): Season?
}