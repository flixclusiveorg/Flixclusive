package com.flixclusive.feature.mobile.library.manage

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.ToggleCapabilityUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsUseCase
import com.flixclusive.feature.mobile.library.common.model.TrackerProvider
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview.Companion.toPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ManageLibraryViewModel @Inject constructor(
    private val libraryListRepository: LibraryListRepository,
    private val getTrackerProviders: GetTrackerProvidersUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
    private val toggleCapability: ToggleCapabilityUseCase,
    private val getTrackerLists: GetTrackerListsUseCase,
    private val trackerListRepository: TrackerListRepository,
) : ViewModel() {
    private var loadListsJob: Job? = null
    private var loadProvidersJob: Job? = null
    private var addLibJob: Job? = null
    private var removeLibJob: Job? = null
    private var removeSelectionJob: Job? = null
    private var verifyAuthJob: Job? = null

    private val _uiState = MutableStateFlow(ManageLibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val selectedLists = mutableStateSetOf<LibraryListWithPreview>()

    private val _lists = MutableStateFlow<Async<List<LibraryListWithPreview>>>(Async.Loading)
    val lists = _lists.asStateFlow()

    private val _trackers = MutableStateFlow<Async<List<TrackerProvider>>>(Async.Loading)
    val trackers = _trackers.asStateFlow()

    private val _trackerErrors = MutableSharedFlow<List<ProviderWithThrowable>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val trackerErrors = _trackerErrors.asSharedFlow()

    private var lastTrackerLoadErrors = emptySet<String>()

    init {
        initialize()
    }

    private fun loadLists(isRefreshing: Boolean = false) {
        if (loadListsJob?.isActive == true) {
            loadListsJob?.cancel()
        }

        loadListsJob = viewModelScope.launch {
            userSessionDataStore.currentUserId
                .filterNotNull()
                .flatMapLatest { userId ->
                    val initialFilter = _uiState.value.selectedFilter
                    val appLists = libraryListRepository
                        .getListsAndItems(userId = userId, sort = initialFilter)
                        .mapLatest { data ->
                            val list = data.fastMap { it.toPreview() }
                            Async.Success(list) as Async<List<LibraryListWithPreview>>
                        }.onStart { emit(Async.Loading) }
                        .catch {
                            errorLog("Failed to fetch library lists for user $userId with filter $initialFilter")
                            errorLog(it)
                            emit(Async.Failure(it))
                        }

                    val trackerLists = getTrackerLists().mapLatest { trackers ->
                        emitTrackerLoadErrors(trackers.errors)

                        val lists = trackers.lists.map {
                            it.list.toPreview(provider = it.provider, ownerId = userId)
                        }

                        if (trackers.isLoading && lists.isEmpty()) {
                            Async.Loading
                        } else {
                            Async.Success(lists)
                        }
                    }

                    combine(
                        appLists,
                        trackerLists,
                        uiState.map { it.selectedFilter }.distinctUntilChanged(),
                        searchQuery
                            .map { it.trim() }
                            .debounce { if (it.isEmpty()) 0L else 800L }
                            .distinctUntilChanged(),
                    ) { app, tracker, filter, query ->
                        val isTrackerLoading = tracker is Async.Loading
                        if (isTrackerLoading && isRefreshing) {
                            _uiState.update { it.copy(isLoadingTrackers = true) }
                            return@combine _lists.value
                        }

                        when (app) {
                            is Async.Loading -> {
                                Async.Loading
                            }

                            is Async.Failure -> {
                                _uiState.update { it.copy(isLoadingTrackers = false) }
                                Async.Failure(app.message, app.cause)
                            }

                            else -> {
                                val comparator = when (filter) {
                                    is LibrarySort.Added -> compareBy<LibraryListWithPreview> { it.list.id }
                                    is LibrarySort.Name -> compareBy { it.name }
                                    is LibrarySort.Modified -> compareBy { it.list.updatedAt }
                                }.run { takeIf { filter.ascending } ?: reversed() }

                                val appData = (app as? Async.Success)?.data ?: emptyList()
                                val trackerData =
                                    (tracker as? Async.Success)?.data?.sortedWith(comparator) ?: emptyList()
                                val all = appData + trackerData

                                val result = if (query.isEmpty()) {
                                    Async.Success(all)
                                } else {
                                    Async.Success(
                                        all
                                            .fastFilter { library ->
                                                library.name.contains(query, ignoreCase = true) ||
                                                    library.description?.contains(query, ignoreCase = true) == true
                                            }.sortedWith(comparator)
                                    )
                                }

                                _uiState.update {
                                    it.copy(
                                        isLoadingTrackers = isTrackerLoading,
                                        isRefreshing = if (!isTrackerLoading &&
                                            isRefreshing
                                        ) {
                                            false
                                        } else {
                                            it.isRefreshing
                                        },
                                    )
                                }
                                result
                            }
                        }
                    }
                }.collectLatest {
                    _lists.value = it
                }
        }
    }

    private fun loadTrackers(isRefreshing: Boolean = false) {
        if (loadProvidersJob?.isActive == true) {
            loadProvidersJob?.cancel()
            verifyAuthJob?.cancel()
        }

        loadProvidersJob = viewModelScope.launch {
            getTrackerProviders()
                .flatMapLatest { state ->
                    when (state) {
                        is Async.Loading -> flowOf<Async<List<TrackerProvider>>>(Async.Loading)

                        is Async.Failure ->
                            flowOf<Async<List<TrackerProvider>>>(Async.Failure(state.message, state.cause))

                        is Async.Success -> {
                            val providers = state.data.filter { it.metadata != null }
                            if (providers.isEmpty()) {
                                return@flatMapLatest flowOf<Async<List<TrackerProvider>>>(
                                    Async.Success(emptyList()),
                                )
                            }

                            channelFlow<Async<List<TrackerProvider>>> {
                                providers.forEach { provider ->
                                    launch { trackerListRepository.loadAuthentication(provider.id) }
                                }

                                combine(
                                    providers.map { provider ->
                                        trackerListRepository
                                            .isAuthenticated(provider.id)
                                            .map { provider to it }
                                    },
                                ) { it.toList() }
                                    .collect { results ->
                                        val trackers = results.mapNotNull { (provider, auth) ->
                                            TrackerProvider(
                                                metadata = provider.metadata ?: return@mapNotNull null,
                                                isTrackerEnabled = provider.isTrackerEnabled,
                                                isAuthenticated = (auth as? Async.Success)?.data == true,
                                            )
                                        }

                                        send(Async.Success(trackers))
                                    }
                            }
                        }
                    }
                }.collectLatest {
                    if (isRefreshing && it is Async.Loading) return@collectLatest

                    _trackers.value = it
                }
        }
    }

    private suspend fun removeLibrary(list: LibraryListWithPreview) {
        val trackerList = list.trackerList
        if (trackerList != null) {
            trackerListRepository.deleteList(trackerList).onFailure {
                errorLog("Failed to delete tracker list ${list.name}")
                errorLog(it)
                addTrackerError(list.provider, it)
            }
        } else {
            libraryListRepository.deleteListById(list.id)
        }
    }

    private fun addTrackerError(provider: ProviderMetadata?, throwable: Throwable) {
        if (provider == null) return

        _trackerErrors.tryEmit(
            listOf(ProviderWithThrowable(provider = provider, throwable = throwable)),
        )
    }

    private fun emitTrackerLoadErrors(errors: List<ProviderWithThrowable>) {
        if (errors.isEmpty()) return

        val keys = errors.mapTo(mutableSetOf()) { it.identity }
        if (keys == lastTrackerLoadErrors) return

        lastTrackerLoadErrors = keys
        _trackerErrors.tryEmit(errors)
    }

    fun initialize(isRefreshing: Boolean = false) {
        if (isRefreshing && _uiState.value.isLoadingTrackers) return
        lastTrackerLoadErrors = emptySet()
        _uiState.update { it.copy(isRefreshing = isRefreshing) }
        loadTrackers(isRefreshing)
        loadLists(isRefreshing)
    }

    fun onTrackerSignIn(tracker: TrackerProvider) {
        if (verifyAuthJob?.isActive == true) return

        verifyAuthJob = viewModelScope.launch {
            trackerListRepository.invalidate(tracker.id)
            trackerListRepository.loadAuthentication(tracker.id, refresh = true)
            trackerListRepository.loadLists(tracker.id, refresh = true)
        }
    }

    fun onToggleTracker(tracker: TrackerProvider) {
        toggleCapability(tracker.id, ProviderCapability.TRACKER)
    }

    fun onUpdateFilter(filter: LibrarySort) {
        val isUpdatingDirection = _uiState.value.selectedFilter == filter

        _uiState.update {
            if (isUpdatingDirection) {
                it.copy(selectedFilter = it.selectedFilter.toggleAscending())
            } else {
                it.copy(selectedFilter = filter)
            }
        }
    }

    fun onRemoveLongClickedLibrary() {
        if (removeLibJob?.isActive == true) return

        val list = _uiState.value.longClickedLibrary
        requireNotNull(list) { "No library selected for removal!" }

        removeLibJob = appDispatchers.ioScope.launch {
            removeLibrary(list)
        }
    }

    fun onRemoveSelection() {
        if (removeSelectionJob?.isActive == true) return

        removeSelectionJob = appDispatchers.ioScope.launch {
            selectedLists.forEach {
                removeLibrary(it)
            }

            selectedLists.clear()
        }
    }

    fun onSaveEdits(list: LibraryListWithPreview) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            val trackerList = list.trackerList
            if (trackerList != null) {
                trackerListRepository
                    .updateList(
                        trackerList.copy(
                            name = list.name,
                            description = list.description,
                        ),
                    ).onFailure {
                        errorLog("Failed to update tracker list ${list.name}")
                        errorLog(it)
                        addTrackerError(list.provider, it)
                    }
            } else {
                libraryListRepository.updateList(
                    LibraryList(
                        id = list.id,
                        ownerId = list.list.ownerId,
                        name = list.name,
                        description = list.description,
                    )
                )
            }

            _uiState.update { state ->
                state.copy(
                    isEditingLibrary = false,
                    longClickedLibrary = null,
                )
            }
        }
    }

    fun onAdd(
        name: String,
        description: String?,
        tracker: TrackerProvider?,
    ) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            if (tracker != null) {
                trackerListRepository
                    .createList(
                        providerId = tracker.id,
                        name = name,
                        description = description,
                    ).onFailure {
                        errorLog("Failed to create tracker list $name")
                        errorLog(it)
                        addTrackerError(tracker.metadata, it)
                    }

                _uiState.update { state ->
                    state.copy(isCreatingLibrary = false)
                }

                return@launch
            } else {
                val list = LibraryList(
                    ownerId = userId,
                    name = name,
                    description = description,
                )

                libraryListRepository.insertList(list)
                _uiState.update { state ->
                    state.copy(isCreatingLibrary = false)
                }
            }
        }
    }

    fun onStartMultiSelecting() {
        _uiState.update { it.copy(isMultiSelecting = true) }
    }

    fun onToggleSelect(item: LibraryListWithPreview) {
        if (selectedLists.contains(item)) {
            selectedLists.remove(item)
        } else {
            selectedLists.add(item)
        }
    }

    fun onUnselectAll() {
        selectedLists.clear()
        _uiState.update {
            it.copy(isMultiSelecting = false)
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearchBar(isVisible: Boolean) {
        _uiState.update { it.copy(isShowingSearchBar = isVisible) }
    }

    fun onToggleOptionsSheet(isVisible: Boolean) {
        _uiState.update { it.copy(isShowingOptionsSheet = isVisible) }
    }

    fun onLongClickItem(library: LibraryListWithPreview?) {
        _uiState.update { it.copy(longClickedLibrary = library) }
    }

    fun onToggleEditDialog(isVisible: Boolean) {
        _uiState.update {
            if (isVisible) {
                it.copy(
                    isShowingOptionsSheet = false,
                    isEditingLibrary = true,
                )
            } else {
                it.copy(
                    isEditingLibrary = false,
                    longClickedLibrary = null,
                )
            }
        }
    }

    fun onToggleCreateDialog(isVisible: Boolean) {
        _uiState.update {
            it.copy(isCreatingLibrary = isVisible)
        }
    }
}

@Stable
internal data class ManageLibraryUiState(
    val isRefreshing: Boolean = false,
    val isLoadingTrackers: Boolean = false,
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val isShowingOptionsSheet: Boolean = false,
    val isCreatingLibrary: Boolean = false,
    val isEditingLibrary: Boolean = false,
    val longClickedLibrary: LibraryListWithPreview? = null,
    val selectedFilter: LibrarySort = LibrarySort.Added(ascending = true),
)

private val ProviderWithThrowable.identity: String
    get() = "${provider.id}|${throwable::class.qualifiedName}|${throwable.message}"

@Stable
internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val provider: ProviderMetadata? = null,
    val previews: List<PreviewPoster>,
    val trackerList: TrackerList? = null,
) {
    val name get() = list.name
    val description get() = list.description
    val id get() = list.id

    val isFromTracker get() = trackerList != null

    companion object {
        fun LibraryListWithItems.toPreview(): LibraryListWithPreview {
            return LibraryListWithPreview(
                list = list,
                itemsCount = items.size,
                previews = items
                    .takeLast(3)
                    .sortedByDescending { it.item.updatedAt }
                    .map { item -> item.metadata.toPreviewPoster() },
            )
        }

        fun TrackerList.toPreview(
            provider: ProviderMetadata,
            ownerId: String
        ): LibraryListWithPreview {
            return LibraryListWithPreview(
                list = LibraryList(
                    id = id,
                    ownerId = ownerId,
                    name = name,
                    description = description?.takeIf { it.isNotEmpty() },
                    createdAt = Date(createdAt ?: System.currentTimeMillis()),
                    updatedAt = Date(updatedAt ?: System.currentTimeMillis()),
                ),
                itemsCount = itemCount ?: -1,
                provider = provider,
                previews = images.take(3).map { it.toPreviewPoster() },
                trackerList = this,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LibraryListWithPreview) return false

        if (id != other.id) return false
        if (provider?.id != other.provider?.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (previews != other.previews) return false

        return true
    }

    override fun hashCode(): Int {
        var result = itemsCount
        result = 31 * result + list.hashCode()
        result = 31 * result + (provider?.hashCode() ?: 0)
        result = 31 * result + previews.hashCode()
        result = 31 * result + isFromTracker.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }
}

@Stable
internal data class PreviewPoster(
    val title: String? = null,
    val posterPath: String?,
) {
    companion object {
        fun DBMedia.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }

        fun MediaMetadata.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }

        fun String.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = null,
                posterPath = this,
            )
        }
    }
}
