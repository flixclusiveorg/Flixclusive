package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.provider.filter.BottomSheetComponent
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.search_history.SearchHistoryRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getDefaultTmdbFilters
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.provider.Status
import com.flixclusive.model.database.SearchHistory
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.provider.ProviderApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchExpandedScreenViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    providerManager: ProviderManager,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    private val providers = providerManager.workingApis
    val providerDataList by derivedStateOf {
        providerManager.providerDataList.fastMapNotNull { data ->
            if (
                data.status != Status.Maintenance
                && data.status != Status.Down
                && providerManager.isProviderEnabled(data.name)
            ) return@fastMapNotNull data

            null
        }
    }

    val searchHistory = searchHistoryRepository.getAllItemsInFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedAppSettings
        )

    val searchResults = mutableStateListOf<FilmSearchItem>()
    var filters by mutableStateOf(getDefaultTmdbFilters())
        private set

    private var searchingJob: Job? = null

    var selectedProviderIndex by mutableIntStateOf(0)
        private set

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var canPaginate by mutableStateOf(false)
        private set
    var pagingState by mutableStateOf(PagingState.IDLE)
        private set
    var error by mutableStateOf<UiText?>(null)
        private set

    var lastQuerySearched by mutableStateOf("")
        private set
    var searchQuery by mutableStateOf("")
        private set

    internal val currentViewType = mutableStateOf(SearchItemViewType.SearchHistory)

    fun onSearch() {
        if (searchingJob?.isActive == true)
            return

        searchingJob = viewModelScope.launch {
            // Reset pagination
            page = 1
            maxPage = 1
            canPaginate = false
            pagingState = PagingState.IDLE
            searchResults.clear()

            lastQuerySearched = searchQuery
            currentViewType.value = SearchItemViewType.Films

            if (searchQuery.isNotEmpty()) {
                searchHistoryRepository.insert(
                    SearchHistory(query = searchQuery)
                )
            }

            paginateItems()
        }
    }

    fun onChangeProvider(index: Int) {
        selectedProviderIndex = index
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
            if (isDonePaginating())
                return@launch

            pagingState = when (page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            when (
                val result = getResponseFromProviderEndpoint()
            ) {
                is Resource.Success -> result.data?.parseResults()
                is Resource.Failure -> {
                    error = result.error
                    pagingState = when (page) {
                        1 -> PagingState.ERROR
                        else -> PagingState.PAGINATING_EXHAUST
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun deleteSearchHistoryItem(item: SearchHistory) {
        viewModelScope.launch {
            searchHistoryRepository.remove(id = item.id)
        }
    }

    private suspend fun getSelectedProvider(): ProviderApi?
        = providers.first().getOrNull(selectedProviderIndex - 1)

    private fun SearchResponseData<FilmSearchItem>.parseResults() {
        val results = results
            .filterNot { it.posterImage == null }

        maxPage = totalPages
        canPaginate = results.size == 20 || page < maxPage

        if (page == 1) {
            searchResults.clear()
        }

        searchResults.addAll(results)

        pagingState = PagingState.IDLE

        if (canPaginate)
            this@SearchExpandedScreenViewModel.page++
    }

    private fun isDonePaginating(): Boolean {
        return page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE) || searchQuery.isEmpty()
    }

    private fun filterOutUiComponentsFromFilterList(): FilterList
        = FilterList(
            filters.fastMap { group ->
                FilterGroup(
                    name = group.name,
                    list = group.list.fastFilter { filter ->
                        filter !is BottomSheetComponent<*>
                    }
                )
            }.sortedByDescending {
                it.isBeingUsed()
            }
        )

    private suspend fun getResponseFromProviderEndpoint(): Resource<SearchResponseData<FilmSearchItem>> {
        val filteredFilters = filterOutUiComponentsFromFilterList()

        return if (selectedProviderIndex == 0) {
            val mediaTypeFilter = filteredFilters.first().first()
            val mediaType = mediaTypeFilter.state as Int

            tmdbRepository.search(
                page = page,
                query = searchQuery,
                filter = mediaType
            )
        } else {
            try {
                val result = withIOContext {
                    getSelectedProvider()!!.search(
                        page = page,
                        title = searchQuery,
                    )
                }

                Resource.Success(result)
            } catch (e: Exception) {
                errorLog(e)
                Resource.Failure(e)
                    .also {
                        error = it.error
                    }
            }
        }
    }
}

