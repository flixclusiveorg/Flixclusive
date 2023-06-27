package com.flixclusive.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.entities.toWatchlistItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.repository.WatchlistRepository
import com.flixclusive.domain.usecase.VideoDataProviderUseCase
import com.flixclusive.presentation.common.VideoDataDialogState
import com.flixclusive.presentation.common.network.NetworkConnectivityObserver
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
class MainSharedViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val tmdbRepository: TMDBRepository,
    private val videoDataProvider: VideoDataProviderUseCase,
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
            updateVideoDataDialogState(VideoDataDialogState.FETCHING)

            val film = filmToWatch ?: _uiState.value.longClickedFilm!!

            val filmToShow = when {
                film.filmType == FilmType.MOVIE && film !is Movie -> {
                    val response = tmdbRepository.getMovie(film.id)

                    if(response !is Resource.Success) {
                        updateVideoDataDialogState(VideoDataDialogState.ERROR)
                        return@launch
                    }

                    response.data!!
                }
                film.filmType == FilmType.TV_SHOW && film !is TvShow -> {
                    val response = tmdbRepository.getTvShow(film.id)

                    if(response !is Resource.Success) {
                        updateVideoDataDialogState(VideoDataDialogState.ERROR)
                        return@launch
                    }

                    response.data!!
                }
                else -> film
            }

            _uiState.update { it.copy(longClickedFilm = filmToShow) }
            _longClickedFilmWatchHistoryItem.update {
                watchHistoryRepository.getWatchHistoryItemById(film.id)
            }

            videoDataProvider(
                film = filmToShow,
                watchHistoryItem = _longClickedFilmWatchHistoryItem.value,
                episode = episode,
                onSuccess = { videoDataDialogState, episodeToPlay ->
                    _videoData.update { videoDataDialogState }
                    _uiState.update { it.copy(episodeToPlay = episodeToPlay) }
                }
            ).collectLatest(::updateVideoDataDialogState)
        }
    }
    
    private fun updateVideoDataDialogState(videoDataDialogState: VideoDataDialogState) {
        when(videoDataDialogState) {
            VideoDataDialogState.ERROR, VideoDataDialogState.UNAVAILABLE -> _uiState.update {
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
        updateVideoDataDialogState(VideoDataDialogState.IDLE)
        _videoData.update { null }
        _longClickedFilmWatchHistoryItem.update { null }
        _uiState.update { it.copy(episodeToPlay = null) }
    }
}