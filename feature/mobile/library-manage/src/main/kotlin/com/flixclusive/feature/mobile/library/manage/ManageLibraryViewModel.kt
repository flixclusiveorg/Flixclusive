package com.flixclusive.feature.mobile.library.manage

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.ToggleProviderUseCase
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview.Companion.toPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.extension.containsAny
import com.flixclusive.model.film.Film
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.tracker.TrackerList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ManageLibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryListRepository: LibraryListRepository,
    private val getTrackerProviders: GetTrackerProvidersUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
    private val toggleProvider: ToggleProviderUseCase,
    private val getProviderPlugin: GetProviderPluginUseCase,
) : ViewModel() {
    private var loadLibrariesJob: Job? = null
    private var loadProvidersJob: Job? = null
    private var addLibJob: Job? = null
    private var removeLibJob: Job? = null
    private var removeSelectionJob: Job? = null
    private var verifyAuthJob: Job? = null

    private val _uiState = MutableStateFlow(ManageLibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val selectedLibraries = mutableStateSetOf<LibraryListWithPreview>()

    private val _libraries = MutableStateFlow<Async<List<LibraryListWithPreview>>>(Async.Loading)
    val libraries = _libraries.asStateFlow()

    private val _trackers = MutableStateFlow<Async<List<TrackerProvider>>>(Async.Loading)
    val trackers = _trackers.asStateFlow()

    private val _trackerRequiresSignIn = MutableSharedFlow<ProviderMetadata>()
    val trackerRequiresSignIn = _trackerRequiresSignIn.asSharedFlow()

    init {
        initialize()
    }

    private fun loadLibraries() {
        if (loadLibrariesJob?.isActive == true) {
            loadLibrariesJob?.cancel()
        }

        loadLibrariesJob = viewModelScope.launch {
            combine(
                flow = userSessionDataStore.currentUserId.filterNotNull(),
                flow2 = uiState.map { it.selectedFilter }.distinctUntilChanged(),
                flow3 = searchQuery
                    .map { it.trim() }
                    .debounce { if (it.isEmpty()) 0L else 800L }
                    .distinctUntilChanged(),
            ) { userId, filter, query ->
                Triple(userId, filter, query)
            }.flatMapLatest { (user, filter, query) ->
                val appLibraries = libraryListRepository
                    .getListsAndItems(userId = user, sort = filter)
                    .mapLatest { data ->
                        val list = data.fastMap { it.toPreview() }
                        Async.Success(list) as Async<List<LibraryListWithPreview>>
                    }
                    .onStart { emit(Async.Loading) }
                    .catch {
                        errorLog("Failed to fetch library lists for user $user with filter $filter")
                        errorLog(it)
                        emit(Async.Failure(it))
                    }

                val trackerLibraries = getTrackerProviders().mapLatest { state ->
                    if (state is Async.Loading) {
                        return@mapLatest Async.Loading
                    } else if (state is Async.Failure) {
                        return@mapLatest Async.Failure(state.message, state.cause)
                    }

                    val providers = (state as Async.Success).data
                    val libraries = getTrackerLibraries(providers)

                    Async.Success(libraries)
                }

                combine(appLibraries, trackerLibraries) { app, tracker ->
                    when {
                        app is Async.Loading || tracker is Async.Loading -> Async.Loading
                        app is Async.Failure -> Async.Failure(app.message, app.cause)
                        tracker is Async.Failure -> Async.Failure(tracker.message, tracker.cause)
                        else -> {
                            val comparator = when (filter) {
                                is LibrarySort.Added -> compareBy<LibraryListWithPreview> { it.list.id }
                                is LibrarySort.Name -> compareBy { it.name }
                                is LibrarySort.Modified -> compareBy { it.list.updatedAt }
                            }.run { takeIf { filter.ascending } ?: reversed() }

                            val sortedTrackerLibraries = (tracker as Async.Success).data.sortedWith(comparator)
                            val all = (app as Async.Success).data + sortedTrackerLibraries
                            if (query.isEmpty()) return@combine Async.Success(all)

                            val results = all
                                .fastFilter { library ->
                                    library.name.contains(query, ignoreCase = true) ||
                                        library.description?.contains(query, ignoreCase = true) == true
                                }
                                .sortedWith(comparator)

                            Async.Success(results)
                        }
                    }
                }
            }.collectLatest { _libraries.value = it }
        }
    }

    private fun loadTrackers() {
        if (loadProvidersJob?.isActive == true) {
            loadProvidersJob?.cancel()
            verifyAuthJob?.cancel()
        }

        loadProvidersJob = viewModelScope.launch {
            getTrackerProviders()
                .mapLatest {
                    when (it) {
                        is Async.Loading -> Async.Loading
                        is Async.Failure -> Async.Failure(it.message, it.cause)
                        is Async.Success -> Async.Success(
                            it.data.mapNotNull { provider ->
                                val isAuthenticated = safeCall {
                                    provider.plugin?.getTrackerApi(context)?.isAuthenticated()
                                } ?: false

                                TrackerProvider(
                                    metadata = provider.metadata ?: return@mapNotNull null,
                                    isEnabled = provider.isEnabled,
                                    isAuthenticated = isAuthenticated,
                                )
                            }
                        )
                    }
                }.collectLatest { _trackers.value = it }
        }
    }

    private suspend fun getTrackerLibraries(providers: List<ProviderResponseWrapper>): List<LibraryListWithPreview> {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val requiredFeatures = setOf(
            TrackerFeature.LISTS_CREATE,
            TrackerFeature.LISTS_DELETE,
            TrackerFeature.LISTS_UPDATE,
            TrackerFeature.LISTS_READ,
            TrackerFeature.LIST_ITEMS_READ,
        )

        return providers.mapNotNull { provider ->
            if (provider.isEnabled) return@mapNotNull null

            val api = safeCall {
                provider.plugin?.getTrackerApi(context)
            } ?: return@mapNotNull null

            if (!api.features.containsAny(requiredFeatures)) return@mapNotNull null
            if (!api.isAuthenticated()) return@mapNotNull null

            val lists = api.getLists()

            lists.mapNotNull { list ->
                try {
                    val items = api.getListItems(list, 1)
                    LibraryListWithPreview(
                        list = LibraryList(
                            id = list.id,
                            ownerId = userId,
                            name = "${provider.name} - ${list.name}",
                            description = list.description,
                        ),
                        itemsCount = list.itemCount ?: -1,
                        provider = provider.metadata,
                        previews = items.results
                            .takeLast(3)
                            .map { item -> item.toPreviewPoster() }
                    )
                } catch (e: Throwable) {
                    errorLog("Failed to fetch tracker list items for list ${list.name} from provider ${provider.metadata?.name}")
                    errorLog(e)
                    null
                }
            }
        }.flatten()
    }

    fun initialize() {
        loadTrackers()
        loadLibraries()
    }

    fun onTrackerSignIn(provider: TrackerProvider) {
        if (verifyAuthJob?.isActive == true) return

        verifyAuthJob = viewModelScope.launch {
            val plugin = getProviderPlugin(provider.id)

            val isAuthenticated = safeCall {
                plugin?.getTrackerApi(context)?.isAuthenticated()
            } ?: false

            if (!isAuthenticated) {
                _trackerRequiresSignIn.emit(provider.metadata)
                return@launch
            }

            _trackers.update { providers ->
                if (providers !is Async.Success) return@update providers

                val updatedProviders = providers.data.fastMap { provider ->
                    if (provider.id != provider.id) {
                        return@fastMap provider
                    }

                    provider.copy(isAuthenticated = isAuthenticated)
                }

                Async.Success(updatedProviders)
            }
        }
    }

    fun onToggleTracker(tracker: TrackerProvider) {
        toggleProvider(tracker.id)
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

        val listId = _uiState.value.longClickedLibrary?.id
        requireNotNull(listId) { "No library selected for removal!" }

        removeLibJob = appDispatchers.ioScope.launch {
            libraryListRepository.deleteListById(listId)
        }
    }

    fun onRemoveSelection() {
        if (removeSelectionJob?.isActive == true) return

        removeSelectionJob = appDispatchers.ioScope.launch {
            selectedLibraries.forEach {
                if (it.isFromTracker) {
                    val providerId = it.provider?.id ?: return@forEach
                    val plugin = getProviderPlugin(providerId) ?: return@forEach
                    val api = plugin.getTrackerApi(context) ?: return@forEach

                    api.deleteList(
                        list = TrackerList(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            itemCount = it.itemsCount,
                            providerId = providerId,
                        )
                    )
                } else {
                    libraryListRepository.deleteListById(it.id)
                }
            }

            selectedLibraries.clear()
        }
    }

    fun onSaveEdits(list: LibraryList) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            libraryListRepository.updateList(list)
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
    ) {
        if (addLibJob?.isActive == true) return

        addLibJob = appDispatchers.ioScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
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

    fun onStartMultiSelecting() {
        _uiState.update { it.copy(isMultiSelecting = true) }
    }

    fun onToggleSelect(item: LibraryListWithPreview) {
        if (selectedLibraries.contains(item)) {
            selectedLibraries.remove(item)
        } else {
            selectedLibraries.add(item)
        }
    }

    fun onUnselectAll() {
        selectedLibraries.clear()
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
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val isShowingOptionsSheet: Boolean = false,
    val isCreatingLibrary: Boolean = false,
    val isEditingLibrary: Boolean = false,
    val longClickedLibrary: LibraryListWithPreview? = null,
    val selectedFilter: LibrarySort = LibrarySort.Added(ascending = true),
)

@Stable
internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val provider: ProviderMetadata? = null,
    val previews: List<PreviewPoster>,
) {
    val name get() = list.name
    val description get() = list.description
    val id get() = list.id

    val isFromTracker get() = provider != null

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
    }
}

@Stable
internal data class PreviewPoster(
    val title: String?,
    val posterPath: String?,
) {
    companion object {
        fun DBFilm.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }

        fun Film.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage,
            )
        }
    }
}

@Stable
internal data class TrackerProvider(
    val isEnabled: Boolean,
    val isAuthenticated: Boolean,
    val metadata: ProviderMetadata,
) {
    val id: String get() = metadata.id
    val name: String get() = metadata.name

    val iconUrl: String? get() = metadata.iconUrl
    val versionName: String get() = metadata.versionName
    val versionCode: Long get() = metadata.versionCode
    val status: ProviderStatus get() = metadata.status
}
