package com.flixclusive.feature.mobile.search

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetSearchProvidersUseCase
import com.flixclusive.feature.mobile.search.SearchUiState.Companion.resetPagination
import com.flixclusive.feature.mobile.search.util.FilterHelper.isBeingUsed
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.BottomSheetComponent
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class SearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
    private val providerRepository: ProviderRepository,
    getSearchProviders: GetSearchProvidersUseCase,
    dataStoreManager: DataStoreManager,
) : ViewModel() {
    private var searchingJob: Job? = null
    private var paginatingJob: Job? = null

    val providers = getSearchProviders().mapLatest { state ->
        if (state is Async.Loading) {
            return@mapLatest Async.Loading
        } else if (state is Async.Failure) {
            return@mapLatest Async.Failure(state.message, state.cause)
        }

        val data = (state as Async.Success).data
            .mapNotNull { provider ->
                SearchProvider(
                    metadata = provider.metadata ?: return@mapNotNull null,
                    isEnabled = provider.isEnabled,
                )
            }
            .sortedBy { it.name }

        Async.Success(data)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Async.Loading,
    )

    val searchHistory = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            searchHistoryRepository.getAllItemsInFlow(ownerId = userId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val showMediaTitles = dataStoreManager
        .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .map { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val searchResults = mutableStateSetOf<MediaMetadata>()
    var filters by mutableStateOf(FilterList())
        private set

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearch() {
        if (searchingJob?.isActive == true || paginatingJob?.isActive == true) return

        searchingJob = viewModelScope.launch {
            val query = _searchQuery.value

            // Reset pagination
            _uiState.update { it.resetPagination(lastQuerySearched = query) }
            searchResults.clear()

            if (query.isNotBlank()) {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                searchHistoryRepository.insert(SearchHistory(query = query, ownerId = userId))
            }

            paginateItems()
        }
    }

    fun onChangeProvider(id: String) {
        viewModelScope.launch {
            val searchApi = getSearchApi(providerId = id) ?: return@launch
            filters = searchApi.filters
        }

        _uiState.update { it.copy(selectedProviderId = id) }
        if (_searchQuery.value.isEmpty()) return

        onSearch()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onUpdateFilters(newFilters: FilterList) {
        filters = newFilters
    }

    fun onChangeView(viewType: SearchViewType) {
        _uiState.update { it.copy(currentViewType = viewType) }
    }

    fun paginateItems() {
        if (paginatingJob?.isActive == true) return

        paginatingJob = viewModelScope.launch {
            if (isDonePaginating()) return@launch

            _uiState.update {
                it.copy(
                    pagingState = PagingState.Loading,
                    error = null,
                )
            }

            val providerId = _uiState.map { it.selectedProviderId }.filterNotNull().first()
            val query = _searchQuery.value
            val page = _uiState.value.page

            when (
                val result = searchOnProvider(
                    providerId = providerId,
                    query = query,
                    page = page,
                )
            ) {
                Async.Loading -> Unit
                is Async.Success -> {
                    val data = result.data
                    val canPaginate = data.results.size == 20 || data.page < data.totalPages
                    if (data.page == 1) {
                        searchResults.clear()
                    }

                    searchResults.addAll(data.results)
                    val pagingState = when {
                        canPaginate -> PagingState.Idle
                        else -> PagingState.Exhausted
                    }

                    _uiState.update {
                        it.copy(
                            page = it.page + 1,
                            maxPage = data.totalPages,
                            canPaginate = canPaginate,
                            pagingState = pagingState,
                        )
                    }
                }

                is Async.Failure -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            pagingState = when (it.page) {
                                1 -> PagingState.Error(result.message)
                                else -> PagingState.Exhausted
                            },
                        )
                    }
                }
            }
        }
    }

    fun deleteSearchHistoryItem(item: SearchHistory) {
        appDispatchers.ioScope.launch {
            searchHistoryRepository.remove(id = item.id)
        }
    }

    /**
     * Checks if pagination should stop based on current state.
     *
     * Returns true if:
     * - The current page is not the first page AND
     *   - Pagination is not allowed OR
     *   - The paging state is idle (indicating no more data to load)
     * - OR the search query is empty.
     * */
    private fun isDonePaginating(): Boolean =
        _uiState.value.let {
            (it.page != 1 && (!it.canPaginate || it.pagingState.isExhausted)) ||
                _searchQuery.value.isEmpty()
        }

    private fun FilterList.removeUiComponentsFromFilterList(): FilterList =
        FilterList(
            fastMap { group ->
                FilterGroup(
                    name = group.name,
                    list = group.list.fastFilter { filter ->
                        filter !is BottomSheetComponent<*>
                    },
                )
            }.sortedByDescending {
                it.isBeingUsed()
            },
        )

    private suspend fun searchOnProvider(
        providerId: String,
        query: String,
        page: Int,
    ): Async<PaginatedMedia<PartialMedia>> {
        val filteredFilters = filters.removeUiComponentsFromFilterList()

        return try {
            val api = getSearchApi(providerId = providerId)
                ?: return Async.Failure(UiText.from(R.string.error_search_api_not_found))

            val result = withContext(appDispatchers.io) {
                api.search(
                    page = page,
                    query = query,
                    filters = filteredFilters,
                )
            }

            Async.Success(result)
        } catch (e: Exception) {
            errorLog(e)
            Async.Failure(e)
        }
    }

    private suspend fun getSearchApi(providerId: String): SearchProviderApi? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()

        val providers = providerRepository.getEnabledProviders(userId)
        val providerPlugin = providers.firstOrNull { it.metadata?.id == providerId }?.plugin
        return providerPlugin?.getSearchApi(context)
    }
}

@Immutable
internal data class SearchUiState(
    val currentViewType: SearchViewType = SearchViewType.Providers,
    val pagingState: PagingState = PagingState.Loading,
    val page: Int = 1,
    val maxPage: Int = 1,
    val canPaginate: Boolean = false,
    val error: UiText? = null,
    val lastQuerySearched: String = "",
    val selectedProviderId: String? = null,
) {
    companion object {
        fun SearchUiState.resetPagination(lastQuerySearched: String) =
            copy(
                pagingState = PagingState.Loading,
                canPaginate = false,
                page = 1,
                maxPage = 1,
                lastQuerySearched = lastQuerySearched,
                currentViewType = SearchViewType.Medias,
                error = null,
            )
    }
}

@Stable
internal data class SearchProvider(
    val metadata: ProviderMetadata,
    val isEnabled: Boolean,
) {
    val id: String get() = metadata.id
    val name: String get() = metadata.name

    val iconUrl: String? get() = metadata.iconUrl
    val versionName: String get() = metadata.versionName
    val versionCode: Long get() = metadata.versionCode
    val status: ProviderStatus get() = metadata.status
}

internal enum class SearchViewType {
    History,
    Providers,
    Medias,
}
