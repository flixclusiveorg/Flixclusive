package com.flixclusive.feature.tv.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.domain.search.GetSearchRecommendedCardsUseCase
import com.flixclusive.model.configuration.catalog.SearchCatalog
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.util.replaceTypeInUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchScreenViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    getSearchRecommendedCardsUseCase: GetSearchRecommendedCardsUseCase
) : ViewModel() {
    val searchResults = mutableStateListOf<FilmSearchItem>()
    val searchSuggestions = mutableStateListOf<String>()

    private var searchingJob: Job? = null
    private var onChangeFilterJob: Job? = null

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(SearchFilter.ALL)
        private set
    var canPaginate by mutableStateOf(false)
        private set
    var pagingState by mutableStateOf(PagingState.IDLE)
        private set

    var searchQuery by mutableStateOf("")
        private set
    var selectedCatalog: SearchCatalog? by mutableStateOf(null)
        private set
    var isError by mutableStateOf(false)
        private set

    val catalogs = getSearchRecommendedCardsUseCase.cards
        .map { value ->
            if (value is Resource.Success) {
                Resource.Success(value.data?.filterNot { it.id == -1 })
            } else value
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

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
        selectedCatalog = null // remove selected catalog

        searchQuery = query
    }

    fun onCatalogChange(item: SearchCatalog) {
        selectedCatalog = item
    }

    fun paginate() {
        when {
            selectedCatalog != null -> getCatalogItems()
            searchQuery.isNotBlank() -> getSearchItems()
            else -> loadRecentlyTrending()
        }
    }

    private fun loadItems(
        callResponse: Resource<SearchResponseData<FilmSearchItem>>,
        onSuccess: SearchResponseData<FilmSearchItem>.() -> Unit
    ) {
        if (page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE))
            return

        pagingState = when (page) {
            1 -> PagingState.LOADING
            else -> PagingState.PAGINATING
        }

        when (callResponse) {
            is Resource.Failure -> {
                pagingState = when (page) {
                    1 -> PagingState.ERROR
                    else -> PagingState.PAGINATING_EXHAUST
                }
            }
            Resource.Loading -> Unit
            is Resource.Success -> {
                callResponse.data?.run(onSuccess)
            }
        }
    }

    private fun getSearchItems() {
        viewModelScope.launch {
            loadItems(
                callResponse = tmdbRepository.search(
                    page = page,
                    query = searchQuery
                ),
                onSuccess = {
                    val results = results
                        .filterNot { it.posterImage == null }

                    maxPage = totalPages
                    canPaginate = results.size == 20 || page < maxPage

                    if (page == 1) {
                        searchResults.clear()

                        searchSuggestions.clear()
                        searchSuggestions.addAll(results.map { it.title }.take(6))
                    }

                    searchResults.addAll(results)

                    pagingState = PagingState.IDLE

                    if (canPaginate)
                        this@SearchScreenViewModel.page++
                }
            )
        }
    }

    private fun getCatalogItems() {
        viewModelScope.launch {
            val filmTypeCouldBeBoth = selectedCatalog!!.mediaType == "all"
            val urlQuery = if(filmTypeCouldBeBoth && currentFilterSelected != SearchFilter.ALL) {
                selectedCatalog!!.url.replaceTypeInUrl(currentFilterSelected.type)
            } else selectedCatalog!!.url


            loadItems(
                callResponse = tmdbRepository.paginateConfigItems(
                    url = urlQuery,
                    page = page
                ),
                onSuccess = {
                    val results = results
                        .filterNot { it.posterImage == null }

                    maxPage = totalPages
                    canPaginate = results.size == 20 || page < maxPage

                    if (page == 1) {
                        searchResults.clear()
                        searchSuggestions.clear()
                    }

                    searchResults.addAll(results)
                    searchSuggestions.addAll(results.map { it.title })

                    pagingState = PagingState.IDLE

                    if (canPaginate)
                        this@SearchScreenViewModel.page++
                }
            )
        }
    }

    private fun loadRecentlyTrending() {
        viewModelScope.launch {
            val filmType =
                if (currentFilterSelected.type == "multi")
                    "all"
                else currentFilterSelected.type

            loadItems(
                callResponse = tmdbRepository.getTrending(
                    mediaType = filmType,
                    page = page,
                ),
                onSuccess = {
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
                        this@SearchScreenViewModel.page++
                }
            )
        }
    }
}
