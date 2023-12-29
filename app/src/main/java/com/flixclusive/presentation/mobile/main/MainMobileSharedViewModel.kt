package com.flixclusive.presentation.mobile.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.SourceData
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.domain.model.entities.toWatchlistItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.model.tmdb.toFilmInstance
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.SourceLinksProviderUseCase
import com.flixclusive.service.network.NetworkConnectivityObserver
import com.ramcosta.composedestinations.spec.NavGraphSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class MainMobileSharedViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tmdbRepository: TMDBRepository,
    private val sourceLinksProvider: SourceLinksProviderUseCase,
    appSettingsManager: AppSettingsManager,
    networkConnectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    private var onFilmLongClickJob: Job? = null
    private var onWatchlistClickJob: Job? = null
    private var onRemoveFromWatchHistoryJob: Job? = null
    private var onPlayClickJob: Job? = null

    var navGraphThatNeedsToGoToRoot: NavGraphSpec? by mutableStateOf(null)
        private set

    val isConnectedAtNetwork = networkConnectivityObserver
        .connectivityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    private val _uiState = MutableStateFlow(MobileAppUiState())
    val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

    private val _episodeToPlay = MutableStateFlow<TMDBEpisode?>(null)
    val episodeToPlay = _episodeToPlay.asStateFlow()

    private val _filmToPreview = MutableStateFlow<Film?>(null)
    val filmToPreview = _filmToPreview.asStateFlow()

    val loadedSourceData: SourceData?
        get() = _filmToPreview.value?.id?.let {
            sourceLinksProvider.getLinks(
                filmId = it,
                episode = _episodeToPlay.value,
            )
        }

    fun toggleBottomBar(isVisible: Boolean) {
        _uiState.update { it.copy(isShowingBottomNavigationBar = isVisible) }
    }

    fun onFilmLongClick(film: Film) {
        if(onFilmLongClickJob?.isActive == true)
            return

        onFilmLongClickJob = viewModelScope.launch {
            val isInWatchlist = watchlistRepository.getWatchlistItemById(film.id) != null
            val isInWatchHistory = watchHistoryRepository.getWatchHistoryItemById(film.id) != null

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

    fun onNavBarItemClickTwice(navGraph: NavGraphSpec?) {
        navGraphThatNeedsToGoToRoot = navGraph
    }

    fun onWatchlistButtonClick() {
        if(onWatchlistClickJob?.isActive == true)
            return

        onWatchlistClickJob = viewModelScope.launch {
            _filmToPreview.value?.let { film ->
                val isInWatchlist = _uiState.value.isLongClickedFilmInWatchlist
                if(isInWatchlist) {
                    watchlistRepository.removeById(film.id)
                } else {
                    watchlistRepository.insert(film.toWatchlistItem())
                }

                _uiState.update {
                    it.copy(isLongClickedFilmInWatchlist = !isInWatchlist)
                }
            }
        }
    }

    fun onSeeMoreClick(shouldSeeMore: Boolean) {
        _uiState.update {
            it.copy(isSeeingMoreDetailsOfLongClickedFilm = shouldSeeMore)
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

                    watchHistoryRepository.deleteById(film.id)
                }
            }
        }
    }

    fun onPlayClick(film: Film? = null, episode: TMDBEpisode? = null) {
        if(onPlayClickJob?.isActive == true)
            return

        onPlayClickJob = viewModelScope.launch {
            updateVideoDataDialogState(SourceDataState.Fetching(R.string.film_data_fetching))

            var filmToShow = film ?: _filmToPreview.value!!

            val response = filmToShow.run {
                when {
                    filmType == FilmType.MOVIE && this !is Movie -> {
                        tmdbRepository.getMovie(id)
                    }
                    filmType == FilmType.TV_SHOW && this !is TvShow -> {
                        tmdbRepository.getTvShow(id)
                    }
                    else -> Resource.Success(this)
                }
            }

            if(response !is Resource.Success) {
                return@launch updateVideoDataDialogState(SourceDataState.Error(R.string.film_data_fetch_failed))
            }

            filmToShow = response.data ?: return@launch updateVideoDataDialogState(SourceDataState.Unavailable())

            val watchHistoryItem = watchHistoryRepository.getWatchHistoryItemById(filmToShow.id)
                ?.copy(film = filmToShow.toFilmInstance())
                ?.also { item ->
                    viewModelScope.launch {
                        watchHistoryRepository.insert(item)
                    }
                }

            _filmToPreview.value = filmToShow

            sourceLinksProvider.clearCache() // Clear cache for safety.
            sourceLinksProvider.loadLinks(
                film = filmToShow,
                watchHistoryItem = watchHistoryItem,
                episode = episode,
                onSuccess = { episodeToPlay ->
                    _episodeToPlay.value = episodeToPlay
                }
            ).collectLatest(::updateVideoDataDialogState)
        }
    }

    private fun updateVideoDataDialogState(sourceDataState: SourceDataState) {
        _uiState.update {
            it.copy(sourceDataState = sourceDataState)
        }
    }

    fun onConsumePlayerDialog(isForceClosing: Boolean = false) {
        updateVideoDataDialogState(SourceDataState.Idle)
        if(isForceClosing) {
            onPlayClickJob?.cancel() // Cancel job
            onPlayClickJob = null
        }

        _episodeToPlay.value = null
    }

    fun setIsInPlayer(state: Boolean) {
        _uiState.update { it.copy(isInPlayer = state) }
    }

    fun setPiPModeState(state: Boolean) {
        _uiState.update { it.copy(isInPipMode = state) }
    }
}

