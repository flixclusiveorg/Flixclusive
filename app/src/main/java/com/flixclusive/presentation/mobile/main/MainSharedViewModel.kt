package com.flixclusive.presentation.mobile.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.entities.toWatchlistItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.presentation.common.NetworkConnectivityObserver
import com.flixclusive_provider.models.common.VideoData
import com.ramcosta.composedestinations.spec.NavGraphSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainSharedViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tmdbRepository: TMDBRepository,
    private val videoDataProvider: VideoDataProviderUseCase,
    private val appSettings: DataStore<AppSettings>,
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

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _videoData: MutableStateFlow<VideoData?> = MutableStateFlow(null)
    val videoData: StateFlow<VideoData?> = _videoData.asStateFlow()

    private val _longClickedFilmWatchHistoryItem = MutableStateFlow<WatchHistoryItem?>(null)
    val longClickedFilmWatchHistoryItem = _longClickedFilmWatchHistoryItem.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.data
                .mapLatest { it.preferredServer }
                .collectLatest { preferredServer ->
                _uiState.update {
                    it.copy(preferredServer = preferredServer)
                }
            }
        }
    }

    fun onBottomNavigationBarVisibilityChange(newVisibilityValue: Boolean) {
        _uiState.update { it.copy(isShowingBottomNavigationBar = newVisibilityValue) }
    }

    fun onFilmLongClick(film: Film) {
        if(onFilmLongClickJob?.isActive == true)
            return

        onFilmLongClickJob = viewModelScope.launch {
            _longClickedFilmWatchHistoryItem.update { watchHistoryRepository.getWatchHistoryItemById(film.id) }

            val isInWatchlist = watchlistRepository.getWatchlistItemById(film.id) != null
            val isInWatchHistory = _longClickedFilmWatchHistoryItem.value != null

            _uiState.update {
                it.copy(
                    longClickedFilm = film,
                    isShowingBottomSheetCard = true,
                    isLongClickedFilmInWatchlist = isInWatchlist,
                    isLongClickedFilmInWatchHistory = isInWatchHistory
                )
            }
        }
    }

    fun onBottomSheetClose() {
        _uiState.update { currentState ->
            currentState.copy(
                longClickedFilm = null,
                isShowingBottomSheetCard = false
            )
        }
    }

    fun onNavBarItemClickTwice(navGraph: NavGraphSpec?) {
        navGraphThatNeedsToGoToRoot = navGraph
    }

    fun onWatchlistButtonClick() {
        if(onWatchlistClickJob?.isActive == true)
            return

        onWatchlistClickJob = viewModelScope.launch {
            _uiState.value.longClickedFilm?.let { film ->
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
                _uiState.value.longClickedFilm?.let { film ->
                    _uiState.update {
                        it.copy(isLongClickedFilmInWatchHistory = false)
                    }

                    watchHistoryRepository.deleteById(film.id)

                    _longClickedFilmWatchHistoryItem.update { null }
                }
            }
        }
    }

    fun onPlayClick(filmToWatch: Film? = null, episode: TMDBEpisode? = null) {
        if(onPlayClickJob?.isActive == true)
            return

        onPlayClickJob = viewModelScope.launch {
            try {
                updateVideoDataDialogState(VideoDataDialogState.Fetching)

                val film = filmToWatch ?: _uiState.value.longClickedFilm!!

                val response = when {
                    film.filmType == FilmType.MOVIE && film !is Movie -> {
                        tmdbRepository.getMovie(film.id)
                    }
                    film.filmType == FilmType.TV_SHOW && film !is TvShow -> {
                        tmdbRepository.getTvShow(film.id)
                    }
                    else -> Resource.Success(film)
                }

                if(response !is Resource.Success) {
                    return@launch updateVideoDataDialogState(VideoDataDialogState.Error(R.string.film_data_fetch_failed))
                }

                val filmToShow = response.data ?: return@launch updateVideoDataDialogState(VideoDataDialogState.Unavailable())

                _uiState.update { it.copy(longClickedFilm = filmToShow) }
                _longClickedFilmWatchHistoryItem.update {
                    watchHistoryRepository.getWatchHistoryItemById(film.id)
                }

                videoDataProvider(
                    film = filmToShow,
                    watchHistoryItem = _longClickedFilmWatchHistoryItem.value,
                    episode = episode,
                    server = _uiState.value.preferredServer,
                    onSuccess = { videoDataDialogState, episodeToPlay ->
                        _videoData.update { videoDataDialogState }
                        _uiState.update { it.copy(episodeToPlay = episodeToPlay) }
                    }
                ).collectLatest(::updateVideoDataDialogState)
            } catch (_: Exception) {}
        }
    }
    
    private fun updateVideoDataDialogState(videoDataDialogState: VideoDataDialogState) {
        when(videoDataDialogState) {
            is VideoDataDialogState.Error, is VideoDataDialogState.Unavailable -> _uiState.update {
                it.copy(
                    videoDataDialogState = videoDataDialogState,
                    episodeToPlay = null
                )
            }
            else -> _uiState.update {
                it.copy(videoDataDialogState = videoDataDialogState)
            }
        }
    }

    fun onConsumePlayerDialog() {
        updateVideoDataDialogState(VideoDataDialogState.Idle)
        onPlayClickJob?.cancel() // Cancel job
        _videoData.update { null }
        _longClickedFilmWatchHistoryItem.update { null }
        _uiState.update { it.copy(episodeToPlay = null) }
    }
}