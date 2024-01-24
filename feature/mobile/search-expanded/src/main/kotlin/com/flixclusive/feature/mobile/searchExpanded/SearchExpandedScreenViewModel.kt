package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.common.SearchFilter
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.tmdb.TMDBSearchItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchExpandedScreenViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    val searchResults = mutableStateListOf<TMDBSearchItem>()

    private var searchingJob: Job? = null
    private var onChangeFilterJob: Job? = null

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(SearchFilter.ALL)
    var canPaginate by mutableStateOf(false)
    var pagingState by mutableStateOf(PagingState.IDLE)

    var searchQuery by mutableStateOf("")
    var isError by mutableStateOf(false)

    init {
        loadRecentlyTrending()
    }

    fun onSearch() {
        if (searchingJob?.isActive == true)
            return

        searchingJob = viewModelScope.launch {
            // Reset pagination
            page = 1
            maxPage = 1
            canPaginate = false
            pagingState = PagingState.IDLE

            paginate()
        }
    }

    fun onChangeFilter(filter: SearchFilter) {
        if (onChangeFilterJob?.isActive == true)
            return

        onChangeFilterJob = viewModelScope.launch {
            currentFilterSelected = filter
            onSearch()
        }
    }

    fun onErrorChange(isError: Boolean) {
        this.isError = isError
    }

    fun onQueryChange(query: String) {
        searchQuery = query
    }

    fun paginate() {
        if(searchQuery.isNotEmpty()) {
            getSearchItems()
        } else loadRecentlyTrending()
    }

    private fun getSearchItems() {
        viewModelScope.launch {
            if (page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE) || searchQuery.isEmpty())
                return@launch

            pagingState = when (page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            when (
                val result = tmdbRepository.search(
                    mediaType = currentFilterSelected.type,
                    page = page,
                    query = searchQuery
                )
            ) {
                is Resource.Failure -> {
                    pagingState = when (page) {
                        1 -> PagingState.ERROR
                        else -> PagingState.PAGINATING_EXHAUST
                    }
                }

                Resource.Loading -> Unit
                is Resource.Success -> {
                    result.data?.run {
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
                }
            }
        }
    }

    private fun loadRecentlyTrending() {
        viewModelScope.launch {
            if (page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE))
                return@launch

            pagingState = when (page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            val filmType =
                if (currentFilterSelected.type == "multi")
                    "all"
                else currentFilterSelected.type

            when (
                val result = tmdbRepository.getTrending(
                    mediaType = filmType,
                    page = page,
                )
            ) {
                is Resource.Failure -> {
                    pagingState = when (page) {
                        1 -> PagingState.ERROR
                        else -> PagingState.PAGINATING_EXHAUST
                    }
                }
                Resource.Loading -> Unit
                is Resource.Success -> {
                    result.data?.run {
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
                }
            }
        }
    }
}

