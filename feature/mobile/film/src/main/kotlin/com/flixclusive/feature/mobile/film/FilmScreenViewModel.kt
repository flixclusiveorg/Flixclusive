package com.flixclusive.feature.mobile.film

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.FilmScreenNavArgs
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class FilmScreenViewModel @Inject constructor(
    watchProgressRepository: WatchProgressRepository,
    getSeason: GetSeasonUseCase,
    toggleWatchlistStatus: ToggleWatchlistStatusUseCase,
    dataStoreManager: DataStoreManager,
    userSessionManager: UserSessionManager,
    savedStateHandle: SavedStateHandle,
    appDispatchers: AppDispatchers,
    private val providerRepository: ProviderRepository,
    private val getFilmMetadata: GetFilmMetadataUseCase
) : ViewModel() {
    /** The partial film data obtained from nav args */
    private val navArgFilm = savedStateHandle.navArgs<FilmScreenNavArgs>().film

    private var fetchMetadataJob: Job? = null

    private val _uiState = MutableStateFlow(FilmUiState())
    val uiState = _uiState.asStateFlow()

    private val _metadata = MutableStateFlow<FilmMetadata?>(null)
    val metadata = _metadata.asStateFlow()

    private val _season = MutableStateFlow<Resource<Season>?>(null)
    val season = _season.asStateFlow()

    /** Displays the title of the media under the card */
    val showFilmTitles = dataStoreManager
        .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .mapLatest { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    /**
     * The season currently being displayed IF [metadata] is a [TvShow] and a season is selected.
     *
     * This is either the season selected by the user (based on [FilmUiState.selectedSeason]),
     * the last watched season if no season is selected. If neither of those are available,
     * it will be the latest season.
     * */
    val seasonToDisplay = uiState
        .mapLatest { it.selectedSeason }
        .filterNotNull()
        .distinctUntilChanged()
        .mapNotNull {
            val tvShow = _metadata.filterNotNull().take(1).single()
            if (tvShow !is TvShow) return@mapNotNull null

            getSeason(tvShow, it)
        }
        .flattenConcat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * The watch progress entity for the current film and user, if it exists.
     * */
    val watchProgress =
        userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                watchProgressRepository.getAsFlow(
                    ownerId = user.id,
                    id = navArgFilm.identifier,
                    type = navArgFilm.filmType
                )
            }.stateIn(
                scope = appDispatchers.defaultScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    /**
     * Fetches the metadata for the current film.
     * */
    private suspend fun fetchMetadata() {
        // Reset any previous errors and show loading
        _uiState.update {
            it.copy(error = null, isLoading = true)
        }

        if (navArgFilm is FilmMetadata) {
            _metadata.value = navArgFilm
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val response = getFilmMetadata(navArgFilm)
        var error: UiText? = null

        if (response is Resource.Success && response.data != null) {
            _metadata.value = response.data
        } else if (response is Resource.Failure) {
            error = response.error ?: UiText.from(LocaleR.string.error_film_message)
        }

        _uiState.update {
            it.copy(isLoading = false, error = error)
        }
    }

    private fun fetchProviderUsed() {
        val providerId = _metadata.value?.providerId
        val providerUsed = providerId?.let {
            providerRepository.getProviderMetadata(it)?.name
        }

        if (providerUsed == null) {
            _uiState.update {
                it.copy(error = UiText.from(R.string.provider_null_error_message))
            }
            return
        }

        _uiState.update { it.copy(providerUsed = providerUsed) }
    }

    private suspend fun setInitialSelectedSeason() {
        val tvShow  = _metadata.value

        if (tvShow !is TvShow) return

        val episodeProgress = watchProgress.first() as? EpisodeProgressWithMetadata
        val initialSelectedSeason = episodeProgress?.watchData?.seasonNumber ?: tvShow.totalSeasons

        _uiState.update {
            it.copy(selectedSeason = initialSelectedSeason)
        }
    }

    fun onRetry() {
        if (fetchMetadataJob?.isActive == true) return

        fetchMetadataJob = viewModelScope.launch {
            fetchMetadata()
            if (_uiState.value.error != null) return@launch

            setInitialSelectedSeason()
        }
    }

    fun addToLibrary() {
        // TODO: Implement adding to library
    }

    fun onSeasonChange(seasonNumber: Int) {
        _uiState.update {
            it.copy(selectedSeason = seasonNumber)
        }
    }

    fun onConsumeError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    init {
        viewModelScope.launch {
            launch init@ {
                // Fetch the detailed metadata using the navArgs
                // then check for any errors before proceeding.
                fetchMetadata()
                if (_uiState.value.error != null) return@init

                // Fetch the provider this metadata came from
                // then check for any errors before proceeding.
                fetchProviderUsed()
                if (_uiState.value.error != null) return@init

                setInitialSelectedSeason()
            }

            launch {
                seasonToDisplay.collectLatest { seasonState ->
                    val tvShow = _metadata.value

                    if (tvShow !is TvShow || seasonState !is Resource.Success) return@collectLatest

                    // If we got a new season, and it's not in the current film metadata, add it.
                    // This saves us from having to re-fetch the entire film metadata just to get the new season.
                    val season = seasonState.data!!
                    val seasonNumber = tvShow.seasons.binarySearchBy(season.number) { it.number }

                    if (seasonNumber == -1) {
                        _metadata.update {
                            val atomicMetadata = it as TvShow
                            val newSeasons = (atomicMetadata.seasons + season).sortedBy { it.number }

                            atomicMetadata.copy(seasons = newSeasons)
                        }
                    }
                }
            }
        }
    }
}

@Stable
internal data class FilmUiState(
    val selectedSeason: Int? = null,
    val providerUsed: String = DEFAULT_FILM_SOURCE_NAME,
    val error: UiText? = null,
    val isLoading: Boolean = false
) {
    val screenState: FilmScreenState get() {
        return when {
            isLoading -> FilmScreenState.Loading
            error != null -> FilmScreenState.Error
            else -> FilmScreenState.Success
        }
    }
}

enum class FilmScreenState {
    Loading,
    Error,
    Success
}

private val FilmMetadata.hasCollections
    get() = this is Movie && collection?.films?.isNotEmpty() == true
