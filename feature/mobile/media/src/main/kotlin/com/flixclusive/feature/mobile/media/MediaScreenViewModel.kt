package com.flixclusive.feature.mobile.media

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.usecase.download.DownloadTarget
import com.flixclusive.domain.provider.usecase.download.ToggleMediaDownloadUseCase
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsForMediaUseCase
import com.flixclusive.domain.provider.usecase.tracker.SyncFromScrobblersUseCase
import com.flixclusive.feature.mobile.media.LibraryListAndState.Companion.toLibraryState
import com.flixclusive.feature.mobile.media.util.combinedProgress
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.tracker.TrackerList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import kotlinx.coroutines.flow.flowOf
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
    private val appDispatchers: AppDispatchers,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val libraryListRepository: LibraryListRepository,
    private val toggleWatchProgressStatus: ToggleWatchProgressStatusUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    private val getProviderMetadata: GetProviderMetadataUseCase,
    private val getTrackerListsForMedia: GetTrackerListsForMediaUseCase,
    private val trackerListRepository: TrackerListRepository,
    private val getCrossMatchedMediaMetadata: GetCrossMatchedMediaMetadataUseCase,
    private val syncFromScrobblers: SyncFromScrobblersUseCase,
    private val toggleMediaDownload: ToggleMediaDownloadUseCase,
    private val mediaDownloadRepository: MediaDownloadRepository,
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

    private val reportedTrackerErrors = mutableSetOf<String>()

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

    /**
     * Transient overrides layered on top of the persisted [DownloadItem] state — used to show
     * [Async.Loading] while resolving/queueing a download (before any [DownloadItem] row exists
     * yet to read state from) and [Async.Failure] when resolution/queueing fails outright.
     * Cleared once the corresponding [DownloadItem] exists (or on success).
     */
    private val downloadOverrides = MutableStateFlow<Map<DownloadScopeKey, Async<Unit>>>(emptyMap())

    /**
     * The download status shown on [com.flixclusive.feature.mobile.media.component.HeaderButtons]:
     * the movie's own download for a [com.flixclusive.model.media.Movie], or the aggregate of the
     * currently selected season's episodes for a [Show].
     */
    val downloadStatus: StateFlow<Async<MediaDownloadStatus>> = combine(
        _metadata.filterNotNull(),
        uiState.mapLatest { it.selectedSeason }.distinctUntilChanged(),
        downloadOverrides,
    ) { media, season, overrides -> Triple(media, season, overrides) }
        .flatMapLatest { (media, season, overrides) ->
            val key = headerDownloadKey(media, season)
            val itemsFlow = when {
                media is Show && season != null -> mediaDownloadRepository.observeBatch(media.id, season)
                media is Show -> flowOf(emptyList())
                else -> mediaDownloadRepository.observeAllItems().mapLatest { items ->
                    items.filter { it.mediaId == media.id && it.seasonNumber == null }
                }
            }

            itemsFlow.mapLatest { items -> deriveAsyncStatus(items, overrides[key]) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Async.Loading,
        )

    /** Per-episode download status for the currently selected season, keyed by episode number. */
    val episodeDownloadStatuses: StateFlow<Map<Int, Async<MediaDownloadStatus>>> = combine(
        _metadata.filterIsInstance<Show>(),
        uiState.mapLatest { it.selectedSeason }.filterNotNull().distinctUntilChanged(),
        downloadOverrides,
    ) { show, season, overrides -> Triple(show, season, overrides) }
        .flatMapLatest { (show, season, overrides) ->
            mediaDownloadRepository.observeBatch(show.id, season).mapLatest { items ->
                val byEpisode = items.filter { it.episodeNumber != null }.groupBy { it.episodeNumber!! }
                val overrideEpisodes = overrides.keys
                    .filterIsInstance<DownloadScopeKey.Episode>()
                    .filter { it.mediaId == show.id && it.seasonNumber == season }
                    .map { it.episodeNumber }

                (byEpisode.keys + overrideEpisodes).associateWith { episodeNumber ->
                    deriveAsyncStatus(
                        items = byEpisode[episodeNumber].orEmpty(),
                        override = overrides[DownloadScopeKey.Episode(show.id, season, episodeNumber)],
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap(),
        )

    private fun headerDownloadKey(
        media: MediaMetadata,
        season: Int?,
    ): DownloadScopeKey {
        return if (media is Show && season != null) {
            DownloadScopeKey.Season(media.id, season)
        } else {
            DownloadScopeKey.Movie(media.id)
        }
    }

    private fun deriveAsyncStatus(
        items: List<DownloadItem>,
        override: Async<Unit>?,
    ): Async<MediaDownloadStatus> {
        if (override is Async.Loading) return Async.Loading
        if (override is Async.Failure) return Async.Failure(override.message, override.cause)
        return Async.Success(deriveDownloadStatus(items))
    }

    private fun deriveDownloadStatus(items: List<DownloadItem>): MediaDownloadStatus {
        if (items.isEmpty()) return MediaDownloadStatus.NotDownloaded
        if (items.any { !it.state.isTerminal }) {
            // Averaged across the whole batch for a show's season aggregate; a single-item list
            // (movie, or one episode) just reads as that item's own progress.
            val progress = items.map { it.combinedProgress() }.average().toFloat()
            // combinedProgress can only report 0 for a stream whose length was never advertised, so
            // the ring would sit empty for the whole download. Narrow on purpose: a queued item has
            // no total either, but it isn't transferring, and a spinning ring would overstate it.
            val isProgressKnown = items.none {
                it.state == DownloadItemState.DOWNLOADING_STREAM && it.streamTotalBytes <= 0
            }
            return MediaDownloadStatus(
                state = MediaDownloadStatus.DownloadState.IN_PROGRESS,
                progress = progress,
                isProgressKnown = isProgressKnown,
            )
        }
        if (items.all { it.state == DownloadItemState.COMPLETED }) return MediaDownloadStatus.Downloaded
        return MediaDownloadStatus.NotDownloaded
    }

    /** Toggles the download shown on [com.flixclusive.feature.mobile.media.component.HeaderButtons]. */
    fun onToggleDownload() {
        val media = _metadata.value ?: return

        if (media !is Show) {
            toggle(DownloadScopeKey.Movie(media.id), DownloadTarget.Single(media))
            return
        }

        val season = (seasonToDisplay.value as? Async.Success)?.data?.season ?: return
        toggle(
            key = DownloadScopeKey.Season(media.id, season.number),
            target = DownloadTarget.WholeSeason(media, season),
        )
    }

    /** Toggles the download for a single episode, shown on [EpisodeWithProgress]'s episode card. */
    fun onToggleEpisodeDownload(episode: Episode) {
        val show = _metadata.value as? Show ?: return

        toggle(
            key = DownloadScopeKey.Episode(show.id, episode.season, episode.number),
            target = DownloadTarget.Single(show, episode),
        )
    }

    /**
     * Runs [target] while showing progress against [key].
     *
     * The override map is presentation state -- it exists so a card can show a spinner between the
     * tap and the download row appearing -- so it stays here rather than going into the use case.
     */
    private fun toggle(
        key: DownloadScopeKey,
        target: DownloadTarget,
    ) {
        viewModelScope.launch {
            downloadOverrides.update { it + (key to Async.Loading) }

            when (val result = toggleMediaDownload(target)) {
                is Async.Failure -> downloadOverrides.update { it + (key to result) }
                else -> downloadOverrides.update { it - key }
            }
        }
    }

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

            combine(
                appLibraries,
                getTrackerListsForMedia(navArgMedia),
            ) { app, trackers -> app to trackers }
                .collectLatest { (app, trackers) ->
                    val trackerStates = trackers.lists.fastMap {
                        it.list.toLibraryState(
                            containsMedia = it.containsMedia,
                            ownerId = userId,
                            provider = it.provider,
                        )
                    }

                    _libraryLists.value = when (app) {
                        is Async.Loading -> Async.Loading
                        is Async.Failure -> Async.Failure(app.message, app.cause)
                        is Async.Success -> Async.Success(app.data + trackerStates)
                    }

                    reportTrackerErrors(trackers.errors)
                }
        }
    }

    private suspend fun reportTrackerErrors(errors: List<ProviderWithThrowable>) {
        errors.forEach {
            if (!reportedTrackerErrors.add(it.provider.id)) return@forEach
            _trackerError.emit(UiText.from(it.throwable.message ?: "Unknown error"))
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

            val trackerList = list.trackerList
            if (trackerList != null) {
                val matchedMedia = runCatching {
                    getCrossMatchedMediaMetadata(
                        media = media,
                        providerId = trackerList.providerId,
                    )
                }.onFailure { e ->
                    errorLog(e)
                    _trackerError.emit(
                        UiText.from(
                            R.string.failed_to_toggle_item_on_tracker_list,
                            list.name,
                            e.message ?: "Unknown error"
                        )
                    )
                }.getOrNull() ?: return@launch

                val result = if (list.containsMedia) {
                    trackerListRepository.removeItem(
                        mediaId = navArgMedia.id,
                        list = trackerList,
                        media = matchedMedia,
                    )
                } else {
                    trackerListRepository.addItem(
                        mediaId = navArgMedia.id,
                        list = trackerList,
                        media = matchedMedia,
                    )
                }

                result.onFailure { e ->
                    errorLog(e)
                    _trackerError.emit(
                        UiText.from(
                            R.string.failed_to_toggle_item_on_tracker_list,
                            list.name,
                            e.message ?: "Unknown error"
                        )
                    )
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
    val trackerList: TrackerList? = null,
) {
    val id get() = list.id
    val providerId get() = provider?.id
    val type get() = list.listType

    val name get() = list.name

    val list get() = listWithItems.list
    val items get() = listWithItems.items

    val isFromTracker get() = trackerList != null

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
            trackerList = this,
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

/**
 * @param progress 0f–1f, meaningful only while [state] is [DownloadState.IN_PROGRESS] — the
 * combined stream+subtitle progress (see [combinedProgress])
 * of a single item, or the average across every item in a batch (a show's season aggregate).
 */
data class MediaDownloadStatus(
    val state: DownloadState,
    val progress: Float = 0f,
    /** False while a transfer is running whose total size the server never advertised, so [progress]
     * can't mean anything yet and the ring should spin rather than sit at zero. */
    val isProgressKnown: Boolean = true,
) {
    enum class DownloadState {
        NOT_DOWNLOADED,
        IN_PROGRESS,
        DOWNLOADED,
    }

    companion object {
        val NotDownloaded = MediaDownloadStatus(DownloadState.NOT_DOWNLOADED)
        val Downloaded = MediaDownloadStatus(DownloadState.DOWNLOADED, progress = 1f)
    }
}

/** Identifies which download this ViewModel's transient override state (`downloadOverrides`) belongs to. */
private sealed interface DownloadScopeKey {
    data class Movie(
        val mediaId: String
    ) : DownloadScopeKey

    data class Season(
        val mediaId: String,
        val seasonNumber: Int
    ) : DownloadScopeKey

    data class Episode(
        val mediaId: String,
        val seasonNumber: Int,
        val episodeNumber: Int
    ) : DownloadScopeKey
}
