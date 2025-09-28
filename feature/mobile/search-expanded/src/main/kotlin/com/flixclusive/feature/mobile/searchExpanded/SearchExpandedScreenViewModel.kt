package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.data.tmdb.util.TMDBFilters.Companion.getDefaultTMDBFilters
import com.flixclusive.feature.mobile.searchExpanded.SearchUiState.Companion.resetPagination
import com.flixclusive.feature.mobile.searchExpanded.util.Constant.TMDB_PROVIDER_ID
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.BottomSheetComponent
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class SearchExpandedScreenViewModel
    @Inject
    constructor(
        private val tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val userSessionManager: UserSessionManager,
        private val providerApiRepository: ProviderApiRepository,
        private val appDispatchers: AppDispatchers,
        providerRepository: ProviderRepository,
        dataStoreManager: DataStoreManager,
    ) : ViewModel() {
        private var searchingJob: Job? = null
        private var paginatingJob: Job? = null

        val providerMetadataList = providerRepository.getEnabledProviders().toImmutableList()

        private val providerApis = mutableStateListOf<ProviderApi>()
        private val apisChangesHandler = ApiListChangesHandler(providerApis)

        init {
            viewModelScope.launch {
                providerApis.addAll(providerApiRepository.getApis())

                providerApiRepository.observe().collect {
                    apisChangesHandler.handleOperations(it)
                }
            }
        }

        val searchHistory = userSessionManager.currentUser
            .filterNotNull()
            .flatMapLatest { user ->
                searchHistoryRepository.getAllItemsInFlow(ownerId = user.id)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        val showFilmTitles = dataStoreManager
            .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
            .map { it.shouldShowTitleOnCards }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

        var searchResults by mutableStateOf(persistentSetOf<Film>())
            private set

        var filters by mutableStateOf(getDefaultTMDBFilters())
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
                searchResults = searchResults.clear()

                if (query.isNotBlank()) {
                    val userId = userSessionManager.currentUser
                        .filterNotNull()
                        .first()
                        .id
                    searchHistoryRepository.insert(SearchHistory(query = query, ownerId = userId))
                }

                paginateItems()
            }
        }

        fun onChangeProvider(id: String) {
            _uiState.update { it.copy(selectedProviderId = id) }
            onSearch()
        }

        fun onQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun onUpdateFilters(newFilters: FilterList) {
            filters = newFilters
        }

        fun onChangeView(viewType: SearchItemViewType) {
            _uiState.update { it.copy(currentViewType = viewType) }
        }

        fun paginateItems() {
            if (searchingJob?.isActive == true || paginatingJob?.isActive == true) return

            paginatingJob = viewModelScope.launch {
                if (isDonePaginating()) return@launch

                _uiState.update {
                    it.copy(
                        pagingState = PagingDataState.Loading,
                        error = null,
                    )
                }

                when (
                    val result = fetchItemsFromProvider()
                ) {
                    Resource.Loading -> Unit
                    is Resource.Success -> {
                        val data = result.data ?: SearchResponseData(
                            page = 1,
                            totalPages = 1,
                            hasNextPage = false,
                            results = emptyList(),
                        )
                        val canPaginate = data.results.size == 20 || data.page < data.totalPages

                        if (data.page == 1) {
                            searchResults = searchResults.clear()
                        }

                        searchResults = searchResults.addAll(data.results)

                        _uiState.update {
                            it.copy(
                                page = it.page + 1,
                                maxPage = data.totalPages,
                                canPaginate = canPaginate,
                                pagingState = PagingDataState.Success(isExhausted = !canPaginate),
                            )
                        }
                    }

                    is Resource.Failure -> {
                        val errorMessage = result.error ?: UiText.from(LocaleR.string.failed_to_paginate_items)
                        _uiState.update {
                            it.copy(
                                error = errorMessage,
                                pagingState = when (it.page) {
                                    1 -> PagingDataState.Error(errorMessage)
                                    else -> PagingDataState.Success(isExhausted = true)
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
                (it.page != 1 && (!it.canPaginate || it.pagingState.isDone)) ||
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

        private suspend fun fetchItemsFromProvider(): Resource<SearchResponseData<FilmSearchItem>> {
            val filteredFilters = filters.removeUiComponentsFromFilterList()

            val selectedProviderId = _uiState.value.selectedProviderId
            val page = _uiState.value.page
            val query = _searchQuery.value

            return if (selectedProviderId == TMDB_PROVIDER_ID) {
                val mediaTypeFilter = filteredFilters.first().first()
                val mediaType = mediaTypeFilter.state as Int

                tmdbFilmSearchItemsRepository.search(
                    page = page,
                    query = query,
                    filter = mediaType,
                )
            } else {
                try {
                    val api = providerApiRepository.getApi(selectedProviderId)!!
                    val result = withContext(appDispatchers.io) {
                        api.search(page = page, title = query)
                    }

                    Resource.Success(result)
                } catch (e: Exception) {
                    errorLog(e)
                    Resource.Failure(e)
                }
            }
        }
    }

@Immutable
internal data class SearchUiState(
    val currentViewType: SearchItemViewType = SearchItemViewType.History,
    val pagingState: PagingDataState = PagingDataState.Loading,
    val page: Int = 1,
    val maxPage: Int = 1,
    val canPaginate: Boolean = false,
    val error: UiText? = null,
    val selectedProviderId: String = TMDB_PROVIDER_ID,
    val lastQuerySearched: String = "",
) {
    companion object {
        fun SearchUiState.resetPagination(lastQuerySearched: String) =
            copy(
                pagingState = PagingDataState.Loading,
                canPaginate = false,
                page = 1,
                maxPage = 1,
                lastQuerySearched = lastQuerySearched,
                currentViewType = SearchItemViewType.Films,
                error = null,
            )
    }
}

internal enum class SearchItemViewType {
    History,
    Providers,
    Films,
}
