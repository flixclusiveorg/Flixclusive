package com.flixclusive.feature.mobile.media

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncFromScrobblersUseCase
import com.flixclusive.domain.provider.usecase.tracker.ToggleListItemOnTrackerListUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerListItemToggleAction
import com.flixclusive.feature.mobile.media.LibraryListAndState.Companion.toLibraryState
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.tracker.TrackerList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = MediaScreenViewModel.Factory::class)
class MediaScreenViewModel @AssistedInject constructor(
    dataStoreManager: DataStoreManager,
    getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
    @param:ApplicationContext private val context: Context,
    private val appDispatchers: AppDispatchers,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val libraryListRepository: LibraryListRepository,
    private val toggleWatchProgressStatus: ToggleWatchProgressStatusUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    private val getProviderMetadata: GetProviderMetadataUseCase,
    private val getTrackerProviders: GetTrackerProvidersUseCase,
    private val getTrackerLists: GetTrackerListsUseCase,
    private val getProviderPlugin: GetProviderPluginUseCase,
    private val toggleListItemOnTrackerList: ToggleListItemOnTrackerListUseCase,
    private val getCrossMatchedMediaMetadata: GetCrossMatchedMediaMetadataUseCase,
    private val syncFromScrobblers: SyncFromScrobblersUseCase,
    @Assisted private val navArgMedia: MediaMetadata,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navArgs: MediaMetadata): MediaScreenViewModel
    }

    private var fetchMetadataJob: Job? = null
    private var fetchLibrariesJob: Job? = null

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState = _uiState.asStateFlow()

    private val _metadata = MutableStateFlow<MediaMetadata?>(null)
    val metadata = _metadata.asStateFlow()

    private val _librarySheetQuery = MutableStateFlow("")
    val librarySheetQuery = _librarySheetQuery.asStateFlow()

    private val _libraryLists = MutableStateFlow<Async<List<LibraryListAndState>>>(Async.Loading)
    val libraryLists = _libraryLists.asStateFlow()

    private val _trackerError = MutableSharedFlow<UiText>()
    val trackerError = _trackerError.asSharedFlow()

    /**
     * A trigger to retry fetching the season data
     *
     * I know... it's not pretty, but it works for now :D
     */
    private val retrySeasonTrigger = MutableStateFlow(0)

    /** Displays the title of the media under the card */
    val showMediaTitles = dataStoreManager
        .getUserPrefsAsFlow(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .mapLatest { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    /**
     * The season currently being displayed IF [metadata] is a [Show] and a season is selected.
     *
     * This is either the season selected by the user (based on [MediaUiState.selectedSeason]),
     * the last watched season if no season is selected. If neither of those are available,
     * it will be the latest season.
     *
     * The reason why this is on a separate flow to [MediaUiState] is because some series
     * have a large number of seasons, and fetching all these seasons can take a while.
     *
     * By separating this into its own flow, we can avoid blocking the entire screen
     * from being displayed while we fetch the season data.
     * */
    val seasonToDisplay = combine(
        uiState.mapLatest { it.selectedSeason }.filterNotNull().distinctUntilChanged(),
        _metadata.filterNotNull(),
        retrySeasonTrigger,
    ) { selectedSeason, tvShow, _ ->
        if (tvShow !is Show) return@combine null

        tvShow to selectedSeason
    }.filterNotNull()
        .flatMapLatest { (tvShow, selectedSeason) ->
            getSeasonWithWatchProgress(tvShow, selectedSeason)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * The watch progress entity for the current media and user, if it exists.
     * */
    val watchProgress = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            watchProgressRepository
                .getAsFlow(
                    ownerId = userId,
                    id = navArgMedia.id,
                    type = navArgMedia.type,
                ).mapLatest {
                    val progress = it?.watchData
                    when (progress?.isCompleted) {
                        true if progress is EpisodeProgress -> getNextEpisodeProgress(progress)
                        else -> progress
                    }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = null,
        )

    /** search results for the library lists, this is separate to avoid multiple mappings */
    @OptIn(FlowPreview::class)
    val searchResults = librarySheetQuery
        .debounce(800.milliseconds) // wait for the user to stop typing
        .filter { it.isNotEmpty() }
        .flatMapLatest { query ->
            libraryLists.mapLatest { state ->
                if (state !is Async.Success) {
                    return@mapLatest state
                }

                val filtered = state.data.fastFilter { it.name.contains(query, ignoreCase = true) }
                Async.Success(filtered)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Async.Loading
        )

    @OptIn(FlowPreview::class)
    private fun syncWatchProgressFromScrobblers() {
        appDispatchers.ioScope.launch {
            val media = _metadata.filterNotNull().first()
            if (media is Show) {
                seasonToDisplay
                    .debounce(800.milliseconds)
                    .filterNotNull()
                    .collectLatest {
                        if (it !is Async.Success) return@collectLatest

                        val season = it.data.season
                        if (season.episodes.isEmpty()) return@collectLatest

                        season.episodes
                            .chunked(10) // chunk to avoid syncing too many episodes at once, which can cause timeouts
                            .forEach { batch ->
                                batch
                                    .map { episode ->
                                        async {
                                            try {
                                                syncFromScrobblers(
                                                    item = media,
                                                    episode = episode
                                                )
                                            } catch (e: Exception) {
                                                errorLog(
                                                    "Failed to sync scrobble data for ${media.title}'s S${episode.season}E${episode.number}: ${e.message}"
                                                )
                                                e.printStackTrace()
                                            }
                                        }
                                    }.awaitAll()
                            }
                    }
            } else {
                try {
                    syncFromScrobblers(media)
                } catch (e: Exception) {
                    errorLog("Failed to sync scrobble data for movie ${media.title}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun fetchLibraryLists() {
        if (fetchLibrariesJob?.isActive == true) {
            fetchLibrariesJob?.cancel()
        }

        fetchLibrariesJob = viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val appLibraries = libraryListRepository
                .getListsAndItems(userId = userId, sort = LibrarySort.Modified())
                .mapLatest { data ->
                    val list = data.fastMap {
                        it.toLibraryState(mediaId = navArgMedia.id)
                    }
                    Async.Success(list) as Async<List<LibraryListAndState>>
                }.onStart { emit(Async.Loading) }
                .catch {
                    errorLog("Failed to fetch library lists for user $userId")
                    errorLog(it)
                    emit(Async.Failure(it))
                }

            val trackerLists = getTrackerProviders().mapLatest { state ->
                if (state is Async.Loading) {
                    return@mapLatest Async.Loading
                } else if (state is Async.Failure) {
                    return@mapLatest Async.Success(emptyList())
                }

                val providers = (state as Async.Success).data
                val libraries = safeCall {
                    getTrackerLists(providers).fastMapNotNull { list ->
                        val isInList = runCatching {
                            val provider = getProviderPlugin(list.providerId) ?: return@fastMapNotNull null
                            val trackerApi = provider.getTrackerApi(context) ?: return@fastMapNotNull null

                            val media = getCrossMatchedMediaMetadata(
                                media = navArgMedia,
                                providerId = list.providerId,
                            )

                            trackerApi.isInList(list, media)
                        }.onFailure { e ->
                            errorLog(
                                "Failed to check if media is in list [${list.id}] for provider ${list.providerId}: ${e.message}"
                            )
                            e.printStackTrace()
                            _trackerError.emit(UiText.from(e.message ?: "Unknown error"))
                            return@mapLatest Async.Failure(UiText.from(e.message ?: "Unknown error"), e)
                        }.getOrNull()
                            ?: return@fastMapNotNull null

                        list.toLibraryState(
                            containsMedia = isInList,
                            ownerId = userId,
                            provider = providers
                                .fastFirstOrNull { it.id == list.providerId }
                                ?.metadata
                                ?: return@fastMapNotNull null,
                        )
                    }
                } ?: emptyList()

                Async.Success(libraries)
            }

            combine(
                appLibraries,
                trackerLists,
            ) { app, tracker ->
                when {
                    app is Async.Loading || tracker is Async.Loading -> Async.Loading

                    app is Async.Failure -> Async.Failure(app.message, app.cause)

                    tracker is Async.Failure -> Async.Failure(tracker.message, tracker.cause)

                    app is Async.Success && tracker is Async.Success -> Async.Success(
                        (app.data + tracker.data).sortedByDescending { it.list.createdAt.time }
                    )

                    else -> Async.Loading
                }
            }.collectLatest { result ->
                _libraryLists.value = result
            }
        }
    }

    /** Fetches the metadata for the current media. */
    private suspend fun fetchMetadata() {
        _uiState.update {
            it.copy(error = null, isLoading = true)
        }

        if (navArgMedia !is PartialMedia) {
            _metadata.value = navArgMedia
            _uiState.update { it.copy(isLoading = false, error = null) }
            return
        }

        getMediaMetadata(navArgMedia).collect { response ->
            when (response) {
                is Async.Loading -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }

                is Async.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = response.message) }
                }

                is Async.Success -> {
                    _metadata.value = response.data
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
            }
        }
    }

    private suspend fun fetchProviderUsed() {
        val providerId = _metadata.value?.providerId
        val provider = providerId?.let { getProviderMetadata(it) }

        if (provider == null) {
            _uiState.update {
                it.copy(error = UiText.from(R.string.provider_null_error_message))
            }
            return
        }

        _uiState.update { it.copy(provider = provider) }
    }

    private suspend fun setInitialSelectedSeason() {
        val tvShow = _metadata.value

        if (tvShow !is Show) return

        val episodeProgress = watchProgress.first() as? EpisodeProgress
        val initialSelectedSeason = episodeProgress?.seasonNumber ?: 1

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

    fun onRetryFetchLibraries() {
        fetchLibraryLists()
    }

    fun onRetryFetchSeason() {
        retrySeasonTrigger.value += 1
    }

    fun toggleOnLibrary(id: String, list: LibraryListAndState) {
        appDispatchers.ioScope.launch {
            val media = _metadata.value
            requireNotNull(media) {
                "MediaMetadata metadata must be loaded before toggling watch progress"
            }

            if (list.type.isWatched) {
                toggleWatchProgressStatus(media = media)
                return@launch
            }

            if (list.isFromTracker) {
                val provider = getProviderPlugin(list.providerId!!)
                if (provider == null) {
                    errorLog("Failed to get provider plugin for id ${list.providerId}")
                    return@launch
                }

                val trackerApi = provider.getTrackerApi(context)
                if (trackerApi == null) {
                    errorLog("Failed to get tracker API for provider ${provider.id}")
                    return@launch
                }

                val trackerList = TrackerList(
                    id = list.id,
                    name = list.name,
                    providerId = provider.id
                )

                val matchedMedia = getCrossMatchedMediaMetadata(
                    media = media,
                    providerId = provider.id,
                )

                val updatedList = toggleListItemOnTrackerList(
                    list = trackerList,
                    item = matchedMedia,
                    action = if (list.containsMedia) {
                        TrackerListItemToggleAction.REMOVE
                    } else {
                        TrackerListItemToggleAction.ADD
                    }
                ).onFailure { e ->
                    errorLog(e)
                    _trackerError.emit(
                        UiText.from(
                            R.string.failed_to_toggle_item_on_tracker_list,
                            list.name,
                            e.message ?: "Unknown error"
                        )
                    )
                }.getOrNull()
                    ?: return@launch

                _libraryLists.update { state ->
                    when (state) {
                        is Async.Loading, is Async.Failure -> {
                            state
                        }

                        is Async.Success -> {
                            val updatedLists = state.data.toMutableList()
                            val index = updatedLists.indexOfFirst { it.id == list.id }

                            if (index == -1) return@update state

                            val current = updatedLists[index]
                            updatedLists[index] = updatedList.toLibraryState(
                                ownerId = current.list.ownerId,
                                containsMedia = !current.containsMedia,
                                provider = current.provider!!,
                            )

                            Async.Success(updatedLists.toList())
                        }
                    }
                }
            } else {
                val oldItem = list.items.fastFirstOrNull { it.mediaId == navArgMedia.id }

                // If the item already exists, remove it. Otherwise, add it.
                if (oldItem != null) {
                    libraryListRepository.deleteItem(oldItem.itemId)
                } else {
                    libraryListRepository.insertItem(
                        item = LibraryListItem(
                            mediaId = navArgMedia.id,
                            listId = id,
                        ),
                        media = _metadata.value,
                    )
                }
            }
        }
    }

    fun toggleEpisodeOnLibrary(episodeWithProgress: EpisodeWithProgress) {
        appDispatchers.ioScope.launch {
            val media = _metadata.filterNotNull().first()
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val watchProgress = episodeWithProgress.watchProgress
            if (watchProgress == null || !watchProgress.isCompleted) {
                watchProgressRepository.insert(
                    media = media,
                    item = watchProgress?.copy(
                        status = WatchStatus.COMPLETED,
                    ) ?: EpisodeProgress(
                        ownerId = userId,
                        mediaId = media.id,
                        seasonNumber = episodeWithProgress.episode.season,
                        episodeNumber = episodeWithProgress.episode.number,
                        status = WatchStatus.COMPLETED,
                        progress = 0L
                    ),
                )
            } else {
                watchProgressRepository.delete(item = watchProgress.id, type = media.type)
            }
        }
    }

    fun onSeasonChange(season: Season) {
        _uiState.update {
            it.copy(selectedSeason = season.number)
        }
    }

    fun onLibrarySheetQueryChange(query: String) {
        _librarySheetQuery.value = query
    }

    private suspend fun getNextEpisodeProgress(progress: EpisodeProgress): EpisodeProgress {
        val nextEpisode = getNextEpisode(
            show = _metadata.filterIsInstance<Show>().first(),
            season = progress.seasonNumber,
            episode = progress.episodeNumber,
        ) ?: return progress

        _uiState.update { it.copy(selectedSeason = nextEpisode.season) }

        return EpisodeProgress(
            ownerId = progress.ownerId,
            mediaId = progress.mediaId,
            seasonNumber = nextEpisode.season,
            episodeNumber = nextEpisode.number,
            status = WatchStatus.WATCHING,
            progress = 0L,
        )
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
                fetchLibraryLists()
                syncWatchProgressFromScrobblers()
            }

            launch {
                seasonToDisplay.collectLatest { seasonState ->
                    val tvShow = _metadata.value as? Show

                    if (tvShow == null || seasonState !is Async.Success) return@collectLatest

                    val (season) = seasonState.data
                    val seasonFromModel = tvShow.getSeason(season.number) ?: return@collectLatest

                    // If we have the season, but it has no episodes, update it.
                    // This can happen when the initial metadata has seasons without episodes.
                    // We only do this if we don't have any episodes for the season to avoid
                    // overwriting any existing data.
                    if (seasonFromModel is Season.Partial) {
                        _metadata.update { current ->
                            val mutableSeasons = tvShow.seasons.toMutableList()
                            val index = mutableSeasons.binarySearch { it.number.compareTo(season.number) }
                            if (index !in mutableSeasons.indices) return@update current

                            (current as Show).copy(seasons = mutableSeasons.toList())
                        }
                    }
                }
            }
        }
    }
}

@Immutable
data class MediaUiState(
    val selectedSeason: Int? = null,
    val provider: ProviderMetadata? = null,
    val error: UiText? = null,
    val isLoading: Boolean = false,
) {
    val screenState: MediaScreenState
        get() {
            return when {
                isLoading -> MediaScreenState.Loading
                error != null -> MediaScreenState.Error
                else -> MediaScreenState.Success
            }
        }
}

/**
 * A data class that holds a library list along with a boolean indicating
 * whether a specific media is contained within that list.
 * */
@Immutable
data class LibraryListAndState(
    private val listWithItems: LibraryListWithItems,
    val containsMedia: Boolean,
    val images: List<String> = emptyList(),
    val provider: ProviderMetadata? = null,
) {
    val id get() = list.id
    val providerId get() = provider?.id
    val type get() = list.listType

    val name get() = list.name

    val list get() = listWithItems.list
    val items get() = listWithItems.items

    val isFromTracker get() = provider != null

    companion object {
        fun LibraryListWithItems.toLibraryState(
            mediaId: String,
        ): LibraryListAndState {
            return LibraryListAndState(
                listWithItems = this,
                images = items.take(3).fastMap { it.metadata.posterImage }.filterNotNull(),
                containsMedia = items.fastAny { item ->
                    item.mediaId == mediaId
                },
            )
        }

        fun TrackerList.toLibraryState(
            containsMedia: Boolean,
            ownerId: String,
            provider: ProviderMetadata,
        ) = LibraryListAndState(
            listWithItems = LibraryListWithItems(
                list = LibraryList(
                    id = id,
                    name = name,
                    description = description,
                    ownerId = ownerId,
                    updatedAt = updatedAt?.let { Date(it) } ?: Date()
                ),
                items = emptyList(),
            ),
            provider = provider,
            images = images,
            containsMedia = containsMedia,
        )
    }
}

enum class MediaScreenState {
    Loading,
    Error,
    Success,
}
