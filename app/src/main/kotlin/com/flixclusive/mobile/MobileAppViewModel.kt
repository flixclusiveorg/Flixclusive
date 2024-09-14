package com.flixclusive.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.ui.mobile.KeyEventHandler
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.data.watchlist.WatchlistRepository
import com.flixclusive.domain.provider.CachedLinks
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.model.database.toWatchlistItem
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.toFilmInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class MobileAppViewModel @Inject constructor(
    private val filmProviderUseCase: FilmProviderUseCase,
    private val getMediaLinksUseCase: GetMediaLinksUseCase,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val watchlistRepository: WatchlistRepository,
    private val appSettingsManager: AppSettingsManager,
    private val appConfigurationManager: AppConfigurationManager,
    internetMonitor: InternetMonitor,
) : ViewModel() {
    private var onFilmLongClickJob: Job? = null
    private var onWatchlistClickJob: Job? = null
    private var onRemoveFromWatchHistoryJob: Job? = null
    private var onPlayClickJob: Job? = null

    val keyEventHandlers = mutableListOf<KeyEventHandler>()
    var isInPipMode by mutableStateOf(false)

    val webViewDriver = WebViewDriverManager.webView
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isConnectedAtNetwork = internetMonitor
        .isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { internetMonitor.isOnline.first() }
        )

    val isPiPModeEnabled: Boolean
        get() = appSettingsManager.cachedAppSettings.isPiPModeEnabled

    val currentVersionCode: Long
        get() = appConfigurationManager.currentAppBuild?.build ?: -1

    val hasSeenChangelogsForCurrentBuild = appSettingsManager
        .onBoardingPreferences.data
        .map {
            currentVersionCode > it.lastSeenChangelogsVersion
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _uiState = MutableStateFlow(MobileAppUiState())
    val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

    private val _episodeToPlay = MutableStateFlow<Episode?>(null)
    val episodeToPlay = _episodeToPlay.asStateFlow()

    private val _filmToPreview = MutableStateFlow<Film?>(null)
    val filmToPreview = _filmToPreview.asStateFlow()

    val loadedCachedLinks: CachedLinks?
        get() = _filmToPreview.value?.identifier?.let {
            getMediaLinksUseCase.getCache(
                filmId = it,
                episode = _episodeToPlay.value,
            )
        }

    fun hideWebViewDriver() {
        WebViewDriverManager.destroy()
    }

    fun previewFilm(film: Film) {
        if(onFilmLongClickJob?.isActive == true)
            return

        onFilmLongClickJob = viewModelScope.launch {
            val isInWatchlist = watchlistRepository.getWatchlistItemById(film.identifier) != null
            val isInWatchHistory = watchHistoryRepository.getWatchHistoryItemById(film.identifier) != null

            _filmToPreview.update { film }
            _uiState.update {
                it.copy(
                    isShowingBottomSheetCard = true,
                    isLongClickedFilmInWatchlist = isInWatchlist,
                    isLongClickedFilmInWatchHistory = isInWatchHistory
                )
            }
        }
    }

    fun onBottomSheetClose() {
        _filmToPreview.value = null
        _uiState.update {
            it.copy(isShowingBottomSheetCard = false)
        }
    }

    fun onWatchlistButtonClick() {
        if(onWatchlistClickJob?.isActive == true)
            return

        onWatchlistClickJob = viewModelScope.launch {
            _filmToPreview.value?.let { film ->
                val isInWatchlist = _uiState.value.isLongClickedFilmInWatchlist
                if(isInWatchlist) {
                    watchlistRepository.removeById(film.identifier)
                } else {
                    watchlistRepository.insert(film.toWatchlistItem())
                }

                _uiState.update {
                    it.copy(isLongClickedFilmInWatchlist = !isInWatchlist)
                }
            }
        }
    }

    fun onRemoveButtonClick() {
        if(onRemoveFromWatchHistoryJob?.isActive == true)
            return

        onRemoveFromWatchHistoryJob = viewModelScope.launch {
            val isLongClickedFilmInWatchHistory = _uiState.value.isLongClickedFilmInWatchHistory

            if(isLongClickedFilmInWatchHistory) {
                _filmToPreview.value?.let { film ->
                    _uiState.update {
                        it.copy(isLongClickedFilmInWatchHistory = false)
                    }

                    watchHistoryRepository.deleteById(film.identifier)
                }
            }
        }
    }

    fun onPlayClick(
        film: Film? = null,
        episode: Episode? = null,
    ) {
        if(onPlayClickJob?.isActive == true)
            return

        onPlayClickJob = viewModelScope.launch {
            updateVideoDataDialogState(MediaLinkResourceState.Fetching(LocaleR.string.film_data_fetching))

            var filmToShow = film ?: _filmToPreview.value ?: return@launch

            val response = when {
                filmToShow !is FilmDetails -> {
                    filmProviderUseCase(partiallyDetailedFilm = filmToShow)
                }
                else -> Resource.Success(filmToShow)
            }

            val errorFetchingFilm = MediaLinkResourceState.Error(LocaleR.string.film_data_fetch_failed)
            if(response !is Resource.Success) {
                return@launch updateVideoDataDialogState(errorFetchingFilm)
            }

            filmToShow = response.data
                ?: return@launch updateVideoDataDialogState(errorFetchingFilm)

            val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(filmToShow.identifier)
                ?.copy(film = filmToShow.toFilmInstance())
                ?.also { item ->
                    viewModelScope.launch {
                        watchHistoryRepository.insert(item)
                    }
                }

            _filmToPreview.value = filmToShow

            getMediaLinksUseCase(
                film = filmToShow,
                watchHistoryItem = watchHistoryItem,
                episode = episode,
                onSuccess = { episodeToPlay ->
                    _episodeToPlay.value = episodeToPlay
                }
            ).collectLatest(::updateVideoDataDialogState)
        }
    }

    private fun updateVideoDataDialogState(mediaLinkResourceState: MediaLinkResourceState) {
        _uiState.update {
            it.copy(mediaLinkResourceState = mediaLinkResourceState)
        }
    }

    fun onConsumeSourceDataDialog(isForceClosing: Boolean = false) {
        updateVideoDataDialogState(MediaLinkResourceState.Idle)
        if(isForceClosing) {
            onPlayClickJob?.cancel() // Cancel job
            onPlayClickJob = null
        }

        _episodeToPlay.value = null
    }

    fun setPlayerModeState(isInPlayer: Boolean) {
        _uiState.update { it.copy(isOnPlayerScreen = isInPlayer) }
    }

    fun onSaveLastSeenChangelogsVersion(version: Long) {
        viewModelScope.launch {
            appSettingsManager.updateOnBoardingPreferences {
                it.copy(
                    lastSeenChangelogsVersion = version
                )
            }
        }
    }
}

