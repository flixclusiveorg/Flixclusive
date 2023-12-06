package com.flixclusive.presentation.common.viewmodels.film

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.entities.toWatchlistItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.preferences.AppSettingsManager
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.FilmProviderUseCase
import com.flixclusive.domain.usecase.SeasonProviderUseCase
import com.flixclusive.domain.usecase.WatchHistoryItemManagerUseCase
import com.flixclusive.domain.usecase.WatchlistItemManagerUseCase
import com.flixclusive.presentation.navArgs
import com.flixclusive.presentation.tv.utils.ModifierTvUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class FilmScreenViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    private val seasonProvider: SeasonProviderUseCase,
    private val filmProvider: FilmProviderUseCase,
    private val watchHistoryItemManager: WatchHistoryItemManagerUseCase,
    private val watchlistItemManager: WatchlistItemManagerUseCase,
    savedStateHandle: SavedStateHandle,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    private val filmArgs = savedStateHandle.navArgs<FilmScreenNavArgs>()
    private val filmId: Int = filmArgs.film.id
    val filmType: FilmType = filmArgs.film.filmType

    private var initializeJob: Job? = null
    private var onSeasonChangeJob: Job? = null
    private var onWatchlistClickJob: Job? = null

    private val _uiState = MutableStateFlow(FilmUiState())
    val uiState = _uiState.asStateFlow()

    private val _film = MutableStateFlow<Film?>(null)
    val film = _film.asStateFlow()

    val watchHistoryItem = watchHistoryRepository
        .getWatchHistoryItemByIdInFlow(filmId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _currentSeasonSelected = MutableStateFlow<Resource<Season>>(Resource.Loading)
    val currentSeasonSelected = _currentSeasonSelected.asStateFlow()

    var selectedSeasonNumber by mutableIntStateOf(value = 1)

    init {
        initializeData()
    }

    fun initializeData() {
        if(initializeJob?.isActive == true)
            return

        initializeJob = viewModelScope.launch {
            _uiState.update { FilmUiState() }
            _film.update { null }

            filmProvider(
                id = filmId,
                type = filmType,
                onError = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error ?: UiText.StringResource(R.string.error_film_message)
                        )
                    }
                },
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }

                    _film.update { data }

                    if(data != null && filmType == FilmType.TV_SHOW) {
                        val seasonToInitialize =
                            if (watchHistoryItem.value?.episodesWatched.isNullOrEmpty()) 1
                            else watchHistoryItem.value!!.episodesWatched.last().seasonNumber!!

                        onSeasonChange(seasonToInitialize) // Initialize first season
                    }
                }
            )
            isFilmInWatchlist()
        }
    }

    private suspend fun isFilmInWatchlist() {
        _film.value?.let { film ->
            _uiState.update {
                it.copy(isFilmInWatchlist = watchlistItemManager.isInWatchlist(film.id))
            }
        }
    }

    fun onSeasonChange(seasonNumber: Int) {
        if(onSeasonChangeJob?.isActive == true)
            return

        onSeasonChangeJob = viewModelScope.launch {
            selectedSeasonNumber = seasonNumber

            _currentSeasonSelected.update { Resource.Loading }
            _currentSeasonSelected.update {
                val result = seasonProvider(id = _film.value!!.id, seasonNumber = seasonNumber)
                if(result == null)
                    Resource.Failure("Could not fetch season data")
                else {
                    watchHistoryItemManager
                        .updateEpisodeCount(
                            id = _film.value!!.id,
                            seasonNumber = result.seasonNumber,
                            episodeCount = result.episodes.size
                        )
                    Resource.Success(result)
                }
            }
        }
    }

    fun onWatchlistButtonClick() {
        if(onWatchlistClickJob?.isActive == true)
            return

        onWatchlistClickJob = viewModelScope.launch {
            _film.value?.toWatchlistItem()?.let { film ->
                val isInWatchlist = watchlistItemManager.toggleWatchlistStatus(film)
                _uiState.update {
                    it.copy(isFilmInWatchlist = isInWatchlist)
                }
            }
        }
    }

    fun onLastItemFocusChange(row: Int, column: Int) {
        _uiState.update {
            it.copy(lastFocusedItem = ModifierTvUtils.FocusPosition(row, column))
        }
    }
}