package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.pagination.PagingState
import com.flixclusive.core.database.entity.SearchHistory
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getDefaultTmdbFilters
import com.flixclusive.domain.database.repository.SearchHistoryRepository
import com.flixclusive.domain.provider.repository.ProviderApiRepository
import com.flixclusive.domain.provider.repository.ProviderRepository
import com.flixclusive.domain.session.UserSessionManager
import com.flixclusive.feature.mobile.searchExpanded.util.Constant.TMDB_PROVIDER_ID
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.datastore.user.UiPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.BottomSheetComponent
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchExpandedScreenViewModel
    @Inject
    constructor(
        private val tmdbRepository: TMDBRepository,
        private val searchHistoryRepository: SearchHistoryRepository,
        private val userSessionManager: UserSessionManager,
        private val providerApiRepository: ProviderApiRepository,
        providerRepository: ProviderRepository,
        dataStoreManager: DataStoreManager,
    ) : ViewModel() {
        val providerMetadataList by lazy { providerRepository.getEnabledProviders() }
        private val providerApis = mutableStateListOf<ProviderApi>()
        private val apisChangesHandler = ApiListChangesHandler(providerApis)

        private val userId: Int? get() = userSessionManager.currentUser.value?.id
        val searchHistory =
            userSessionManager.currentUser
                .filterNotNull()
                .flatMapLatest { user ->
                    searchHistoryRepository.getAllItemsInFlow(ownerId = user.id)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        val uiPreferences =
            dataStoreManager
                .getUserPrefs<UiPreferences>(UserPreferences.UI_PREFS_KEY)
                .asStateFlow(viewModelScope)

        val searchResults = mutableStateListOf<FilmSearchItem>()
        var filters by mutableStateOf(getDefaultTmdbFilters())
            private set

        private var searchingJob: Job? = null

        var selectedProviderId by mutableStateOf<String>(TMDB_PROVIDER_ID)
            private set

        private var page by mutableIntStateOf(1)
        private var maxPage by mutableIntStateOf(1)
        var canPaginate by mutableStateOf(false)
            private set
        var pagingState by mutableStateOf(com.flixclusive.core.common.pagination.PagingState.IDLE)
            private set
        var error by mutableStateOf<UiText?>(null)
            private set

        var lastQuerySearched by mutableStateOf("")
            private set
        var searchQuery by mutableStateOf("")
            private set

        val currentViewType = mutableStateOf(SearchItemViewType.SearchHistory)

        init {
            viewModelScope.launch {
                providerApis.addAll(providerApiRepository.getApis())

                providerApiRepository.observe().collect {
                    apisChangesHandler.handleOperations(it)
                }
            }
        }

        fun onSearch() {
            if (searchingJob?.isActive == true) {
                return
            }

            searchingJob =
                viewModelScope.launch {
                    // Reset pagination
                    page = 1
                    maxPage = 1
                    canPaginate = false
                    pagingState = com.flixclusive.core.common.pagination.PagingState.IDLE
                    searchResults.clear()

                    lastQuerySearched = searchQuery
                    currentViewType.value = SearchItemViewType.Films

                    if (searchQuery.isNotEmpty()) {
                        searchHistoryRepository.insert(
                            SearchHistory(
                                query = searchQuery,
                                ownerId = userId ?: return@launch,
                            ),
                        )
                    }

                    paginateItems()
                }
        }

        fun onChangeProvider(id: String) {
            selectedProviderId = id
            onSearch()
        }

        fun onQueryChange(query: String) {
            searchQuery = query
        }

        fun onUpdateFilters(newFilters: FilterList) {
            filters = newFilters
        }

        fun paginateItems() {
            viewModelScope.launch {
                if (isDonePaginating()) {
                    return@launch
                }

                pagingState =
                    when (page) {
                        1 -> com.flixclusive.core.common.pagination.PagingState.LOADING
                        else -> com.flixclusive.core.common.pagination.PagingState.PAGINATING
                    }

                when (
                    val result = getResponseFromProviderEndpoint()
                ) {
                    is Resource.Success -> {
                        result.data?.parseResults()
                    }

                    is Resource.Failure -> {
                        error = result.error
                        pagingState =
                            when (page) {
                                1 -> com.flixclusive.core.common.pagination.PagingState.ERROR
                                else -> com.flixclusive.core.common.pagination.PagingState.EXHAUSTED
                            }
                    }

                    Resource.Loading -> {
                        Unit
                    }
                }
            }
        }

        fun deleteSearchHistoryItem(item: SearchHistory) {
            viewModelScope.launch {
                searchHistoryRepository.remove(
                    id = item.id,
                    ownerId = userId ?: return@launch,
                )
            }
        }

        private fun SearchResponseData<FilmSearchItem>.parseResults() {
            val results =
                results
                    .filterNot { it.posterImage == null }

            maxPage = totalPages
            canPaginate = results.size == 20 || page < maxPage

            if (page == 1) {
                searchResults.clear()
            }

            searchResults.addAll(results)

            pagingState = PagingState.IDLE

            if (canPaginate) {
                this@SearchExpandedScreenViewModel.page++
            }
        }

        private fun isDonePaginating(): Boolean =
            page != 1 && (page == 1 || !canPaginate || pagingState != com.flixclusive.core.common.pagination.PagingState.IDLE) || searchQuery.isEmpty()

        private fun filterOutUiComponentsFromFilterList(): FilterList =
            FilterList(
                filters
                    .fastMap { group ->
                        FilterGroup(
                            name = group.name,
                            list =
                                group.list.fastFilter { filter ->
                                    filter !is BottomSheetComponent<*>
                                },
                        )
                    }.sortedByDescending {
                        it.isBeingUsed()
                    },
            )

        private suspend fun getResponseFromProviderEndpoint(): Resource<SearchResponseData<FilmSearchItem>> {
            val filteredFilters = filterOutUiComponentsFromFilterList()

            return if (selectedProviderId == TMDB_PROVIDER_ID) {
                val mediaTypeFilter = filteredFilters.first().first()
                val mediaType = mediaTypeFilter.state as Int

                tmdbRepository.search(
                    page = page,
                    query = searchQuery,
                    filter = mediaType,
                )
            } else {
                try {
                    val api = providerApiRepository.getApi(selectedProviderId)!!
                    val result =
                        withIOContext {
                            api.search(
                                page = page,
                                title = searchQuery,
                            )
                        }

                    Resource.Success(result)
                } catch (e: Exception) {
                    errorLog(e)
                    Resource
                        .Failure(e)
                        .also {
                            error = it.error
                        }
                }
            }
        }
    }
