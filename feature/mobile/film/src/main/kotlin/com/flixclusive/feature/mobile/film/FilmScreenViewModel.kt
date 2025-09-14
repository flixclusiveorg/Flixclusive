package com.flixclusive.feature.mobile.film

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.navigation.navargs.FilmScreenNavArgs
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.feature.mobile.film.util.LibraryListMapper.toWatchProgressLibraryList
import com.flixclusive.feature.mobile.film.util.LibraryListMapper.toWatchlistLibraryList
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class FilmScreenViewModel
    @Inject
    constructor(
        context: Context,
        dataStoreManager: DataStoreManager,
        getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
        savedStateHandle: SavedStateHandle,
        private val userSessionManager: UserSessionManager,
        private val appDispatchers: AppDispatchers,
        private val getFilmMetadata: GetFilmMetadataUseCase,
        private val libraryListRepository: LibraryListRepository,
        private val providerRepository: ProviderRepository,
        private val toggleWatchProgressStatus: ToggleWatchProgressStatusUseCase,
        private val toggleWatchlistStatus: ToggleWatchlistStatusUseCase,
        private val watchProgressRepository: WatchProgressRepository,
        private val watchlistRepository: WatchlistRepository,
    ) : ViewModel() {
        /** The partial film data obtained from nav args */
        private val navArgFilm = savedStateHandle.navArgs<FilmScreenNavArgs>().film

        private var fetchMetadataJob: Job? = null

        private val _uiState = MutableStateFlow(FilmUiState())
        val uiState = _uiState.asStateFlow()

        private val _metadata = MutableStateFlow<FilmMetadata?>(null)
        val metadata = _metadata.asStateFlow()

        /**
         * A trigger to retry fetching the season data
         *
         * I know... it's not pretty, but it works for now :D
         */
        private val retrySeasonTrigger = MutableStateFlow(0)

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
         *
         * The reason why this is on a separate flow to [FilmUiState] is because some series
         * have a large number of seasons, and fetching all these seasons can take a while.
         *
         * By separating this into its own flow, we can avoid blocking the entire screen
         * from being displayed while we fetch the season data.
         * */
        val seasonToDisplay = combine(
            uiState.mapLatest { it.selectedSeason }.filterNotNull().distinctUntilChanged(),
            _metadata.filterNotNull(),
            retrySeasonTrigger
        ) { selectedSeason, filmMetadata, _ ->
            selectedSeason to filmMetadata
        }.mapLatest { (selectedSeason, tvShow) ->
            if (tvShow !is TvShow) return@mapLatest null

            getSeasonWithWatchProgress(tvShow, selectedSeason)
        }.filterNotNull()
            .flattenConcat()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

        /**
         * The watch progress entity for the current film and user, if it exists.
         * */
        val watchProgress = userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                watchProgressRepository.getAsFlow(
                    ownerId = user.id,
                    id = navArgFilm.identifier,
                    type = navArgFilm.filmType,
                )
            }.map { it?.watchData }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

        /**
         * This is a distinct flow that represents the current search query
         *
         * This is separated from [FilmUiState] to avoid unnecessary recompositions
         * of the entire screen when the query changes.
         * */
        private val _librarySheetQuery = MutableStateFlow("")
        val librarySheetQuery = _librarySheetQuery.asStateFlow()

        /** lists that contain the current film along with whether they contain it or not */
        val libraryLists = userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                combine(
                    libraryListRepository.getUserWithListsAndItems(user.id).mapLatest { it.lists },
                    watchProgressRepository.getAllAsFlow(user.id),
                    watchlistRepository.getAllAsFlow(user.id),
                ) { lists, watchProgressList, watchlist ->
                    val filmId = navArgFilm.identifier

                    // Pre-process watch progress list
                    val preProcessedWatchProgress = watchProgressList.toWatchProgressLibraryList(context)

                    // Pre-process watchlist
                    val preProcessedWatchlist = watchlist.toWatchlistLibraryList(context)

                    val combinedLists = lists + preProcessedWatchProgress + preProcessedWatchlist

                    combinedLists.fastMap { list ->
                        val containsFilm = list.items.fastAny { item ->
                            item.filmId == filmId
                        }

                        LibraryListAndState(
                            listWithItems = list,
                            containsFilm = containsFilm,
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        /** search results for the library lists, this is separate to avoid multiple mappings */
        @OptIn(FlowPreview::class)
        val searchResults = librarySheetQuery
            .debounce(800) // wait for the user to stop typing
            .filter { it.isNotEmpty() }
            .flatMapLatest { query ->
                libraryLists.mapLatest { lists ->
                    lists.fastFilter { it.list.name.contains(query, true) }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = libraryLists.value,
            )

        /** Fetches the metadata for the current film. */
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
                providerRepository.getProviderMetadata(it)
            }

            if (providerUsed == null) {
                _uiState.update {
                    it.copy(error = UiText.from(R.string.provider_null_error_message))
                }
                return
            }

            _uiState.update { it.copy(provider = providerUsed) }
        }

        private suspend fun setInitialSelectedSeason() {
            val tvShow = _metadata.value

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

        fun onRetryFetchSeason() {
            retrySeasonTrigger.value += 1
        }

        /**
         * Toggles the presence of the current film in the library list with the given [id].
         *
         * If the film is already in the list, it will be removed. If it is not in the list, it will be added.
         *
         * @param id The ID of the library list to toggle the film in.
         * */
        fun toggleOnLibrary(id: Int) {
            viewModelScope.launch(appDispatchers.io) {
                val film = _metadata.value
                requireNotNull(film) {
                    "Film metadata must be loaded before toggling watch progress"
                }

                if (id == LibraryListUtil.WATCHLIST_LIB_ID) {
                    toggleWatchlistStatus(film = film)
                    return@launch
                }

                if (id == LibraryListUtil.WATCH_PROGRESS_LIB_ID) {
                    toggleWatchProgressStatus(film = film)
                    return@launch
                }

                val oldItem = libraryLists.value
                    .firstOrNull { it.list.id == id }
                    ?.items
                    ?.firstOrNull { it.filmId == navArgFilm.identifier }

                // If the item already exists, remove it. Otherwise, add it.
                if (oldItem != null) {
                    libraryListRepository.deleteItem(oldItem.itemId)
                } else {
                    libraryListRepository.insertItem(
                        item = LibraryListItem(
                            filmId = navArgFilm.identifier,
                            listId = id,
                        ),
                        film = _metadata.value,
                    )
                }
            }
        }


        /**
         * Creates a new library list with the given [name] and optional [description],
         * and immediately adds the current film to it.
         *
         * @param name The name of the new library list.
         * @param description An optional description for the new library list.
         * */
        fun createLibrary(name: String, description: String?) {
            val film = _metadata.value
            val user = userSessionManager.currentUser.value

            requireNotNull(film) {
                "Film metadata must be loaded before creating a library"
            }

            requireNotNull(user) {
                "User must be logged in to create a library"
            }

            appDispatchers.ioScope.launch {
                val newListId = libraryListRepository.insertList(
                    list = LibraryList(
                        name = name,
                        description = description,
                        ownerId = user.id,
                    )
                )

                // Immediately add the film to the newly created list
                libraryListRepository.insertItem(
                    item = LibraryListItem(filmId = film.identifier, listId = newListId),
                    film = film,
                )
            }

        }

        fun onSeasonChange(seasonNumber: Int) {
            _uiState.update {
                it.copy(selectedSeason = seasonNumber)
            }
        }

        fun onLibrarySheetQueryChange(query: String) {
            _librarySheetQuery.value = query
        }

        init {
            viewModelScope.launch {
                launch init@{
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
                        val tvShow = _metadata.value as? TvShow

                        if (tvShow == null || seasonState !is Resource.Success) return@collectLatest

                        val (season) = seasonState.data!! // De-structure the season from SeasonWithProgress
                        val seasonNumber = tvShow.seasons.binarySearchBy(season.number) { it.number }

                        // If we have the season but it has no episodes, update it.
                        // This can happen when the initial metadata has seasons without episodes.
                        // We only do this if we don't have any episodes for the season to avoid
                        // overwriting any existing data.
                        val episodes = tvShow.seasons.getOrNull(seasonNumber)?.episodes
                        if (episodes?.isEmpty() == true) {
                            _metadata.update {
                                val mutableSeasons = tvShow.seasons.toMutableList()
                                mutableSeasons[seasonNumber] = season

                                tvShow.copy(seasons = mutableSeasons.toList())
                            }
                        }
                    }
                }
            }
        }
    }

@Immutable
internal data class FilmUiState(
    val selectedSeason: Int? = null,
    val provider: ProviderMetadata? = null,
    val error: UiText? = null,
    val isLoading: Boolean = false,
) {
    val screenState: FilmScreenState
        get() {
            return when {
                isLoading -> FilmScreenState.Loading
                error != null -> FilmScreenState.Error
                else -> FilmScreenState.Success
            }
        }
}

/**
 * A data class that holds a library list along with a boolean indicating
 * whether a specific film is contained within that list.
 * */
@Immutable
internal data class LibraryListAndState(
    private val listWithItems: LibraryListWithItems,
    val containsFilm: Boolean,
) {
    val list get() = listWithItems.list
    val items get() = listWithItems.items
}

internal enum class FilmScreenState {
    Loading,
    Error,
    Success,
}
