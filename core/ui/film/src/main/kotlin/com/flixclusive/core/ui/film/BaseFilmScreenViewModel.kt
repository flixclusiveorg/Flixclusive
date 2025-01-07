package com.flixclusive.core.ui.film

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.tmdb.GetFilmMetadataUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.database.toWatchlistItem
import com.flixclusive.model.datastore.user.UiPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR

/*
* TODO: Remove this ugly ass code
* */
abstract class BaseFilmScreenViewModel(
    private val partiallyDetailedFilm: Film,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val seasonProvider: SeasonProviderUseCase,
    private val filmProvider: GetFilmMetadataUseCase,
    private val toggleWatchlistStatusUseCase: ToggleWatchlistStatusUseCase,
    private val userSessionManager: UserSessionManager,
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private val filmId: String = partiallyDetailedFilm.identifier

    private var initializeJob: Job? = null

    private var onSeasonChangeJob: Job? = null
    private var toggleAsWatchList: Job? = null

    private val _film = MutableStateFlow<Film?>(null)
    val film = _film.asStateFlow()

    private val _uiState = MutableStateFlow(FilmUiState())
    val uiState = _uiState.asStateFlow()

    private val _currentSeasonSelected = MutableStateFlow<Resource<Season>>(Resource.Loading)
    val currentSeasonSelected = _currentSeasonSelected.asStateFlow()

    var selectedSeasonNumber by mutableIntStateOf(value = 1)

    val uiPreferences = dataStoreManager
        .getUserPrefs<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .asStateFlow(viewModelScope)

    val watchHistoryItem = userSessionManager.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            watchHistoryRepository.getWatchHistoryItemByIdInFlow(
                itemId = filmId,
                ownerId = user.id
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        initializeData(film = partiallyDetailedFilm)
        viewModelScope.launch {
            combine(
                _film,
                userSessionManager.currentUser
            ) { film, user ->
                if (film != null && user != null) {
                    val isInWatchlist = toggleWatchlistStatusUseCase
                        .isInWatchlist(film.identifier, user.id)

                    _uiState.update { it.copy(isFilmInWatchlist = isInWatchlist) }
                }
            }.distinctUntilChanged()
                .collect()
        }
    }

    fun initializeData(film: Film = partiallyDetailedFilm) {
        val isSameFilm = filmId == _film.value?.identifier
                && _uiState.value.errorMessage == null
                && !_uiState.value.isLoading

        if (initializeJob?.isActive == true || isSameFilm)
            return

        initializeJob = viewModelScope.launch {
            if (film is FilmMetadata) {
                onSuccessResponse(film)
                return@launch
            }

            _uiState.update { FilmUiState() }
            when (
                val result = filmProvider(partiallyDetailedFilm = film)
            ) {
                is Resource.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false, errorMessage = result.error
                                ?: UiText.StringResource(LocaleR.string.error_film_message)
                        )
                    }
                }
                Resource.Loading -> Unit
                is Resource.Success -> onSuccessResponse(result.data!!)
            }
        }
    }

    private fun onSuccessResponse(response: FilmMetadata) {
        _film.update { response }
        _uiState.update {
            it.copy(
                isLoading = false, errorMessage = null
            )
        }

        if (response.filmType == FilmType.TV_SHOW) {
            var seasonToInitialize = watchHistoryItem.value?.episodesWatched?.lastOrNull()?.seasonNumber

            if (seasonToInitialize == null) {
                seasonToInitialize = (_film.value as TvShow).seasons.firstOrNull()?.number ?: 1
            }

            onSeasonChange(seasonToInitialize)
        }
    }

    fun onSeasonChange(seasonNumber: Int) {
        if (onSeasonChangeJob?.isActive == true || selectedSeasonNumber == seasonNumber && _currentSeasonSelected.value is Resource.Success)
            return

        onSeasonChangeJob = viewModelScope.launch {
            selectedSeasonNumber = seasonNumber

            seasonProvider.asFlow(
                tvShow = _film.value as TvShow,
                seasonNumber = seasonNumber
            ).collectLatest { result ->
                if (result is Resource.Success) {
                    watchHistoryItem.value?.let { item ->
                        result.data?.episodes?.size?.let {
                            val newEpisodesMap = item.episodes.toMutableMap()
                            newEpisodesMap[seasonNumber] = it

                            watchHistoryRepository.insert(item.copy(episodes = newEpisodesMap))
                        }
                    }
                }

                _currentSeasonSelected.value = result
            }
        }
    }

    fun toggleAsWatchList() {
        if (toggleAsWatchList?.isActive == true)
            return

        toggleAsWatchList = viewModelScope.launch {
            val userId = userSessionManager.currentUser.first()?.id ?: return@launch
            val currentFilm = _film.value?.toWatchlistItem(userId) ?: return@launch

            try {
                val isInWatchlist = toggleWatchlistStatusUseCase(
                    watchlistItem = currentFilm, ownerId = userId
                )
                _uiState.update { it.copy(isFilmInWatchlist = isInWatchlist) }
            } catch (e: Exception) {
                // Handle error - maybe update UI state with error message
                if (e.message != null) {
                    _uiState.update {
                        it.copy(errorMessage = UiText.StringValue(e.message!!))
                    }
                }
            }
        }
    }
}