package com.flixclusive.presentation.tv.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.presentation.common.PagingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchTvScreenViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository
) : ViewModel() {
    val searchResults = mutableStateListOf<TMDBSearchItem>()

    private var searchingJob: Job? = null

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var canPaginate by mutableStateOf(false)
    var pagingState by mutableStateOf(PagingState.IDLE)

    var searchQuery by mutableStateOf("")

    fun getSearchItems() {
        viewModelScope.launch {
            if(page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE) || searchQuery.isEmpty())
                return@launch

            pagingState = when(page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            when(
                val result = tmdbRepository.search(
                    mediaType = "multi",
                    page = page,
                    query = searchQuery
                )
            ) {
                is Resource.Failure -> {
                    pagingState = when(page) {
                        1 -> PagingState.ERROR
                        else -> PagingState.PAGINATING_EXHAUST
                    }
                }
                Resource.Loading -> Unit
                is Resource.Success -> {
                    val results = result.data!!.results
                        .filterNot { it is TMDBSearchItem.PersonTMDBSearchItem }
                        .filterNot { it.posterImage == null }

                    maxPage = result.data.totalPages
                    canPaginate = results.size == 20 || page < maxPage

                    if(page == 1) {
                        searchResults.clear()
                    }

                    searchResults.addAll(results)

                    pagingState = PagingState.IDLE

                    if(canPaginate)
                        page++
                }
            }
        }
    }

    fun onSearchClick() {
        if(searchingJob?.isActive == true)
            return

        searchingJob = viewModelScope.launch {
            // Reset pagination
            page = 1
            maxPage = 1
            canPaginate = false
            pagingState = PagingState.IDLE

            getSearchItems()
        }
    }

    fun onQueryChange(query: String) {
        searchQuery = query
    }
}
