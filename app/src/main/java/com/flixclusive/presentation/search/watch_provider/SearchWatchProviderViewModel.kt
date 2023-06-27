package com.flixclusive.presentation.search.watch_provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.destinations.SearchWatchProviderScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchWatchProviderViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = SearchWatchProviderScreenDestination.argsFrom(savedStateHandle)
    val films = mutableStateListOf<TMDBSearchItem>()

    private var onChangeFilterJob: Job? = null

    private var page by mutableStateOf(1)
    private var maxPage by mutableStateOf(1)
    var currentFilterSelected by mutableStateOf(
        if(args.item.isCompany) FilmType.MOVIE else FilmType.TV_SHOW
    )
        private set
    var canPaginate by mutableStateOf(false)
        private set
    var pagingState by mutableStateOf(PagingState.IDLE)
        private set

    init {
        getFilms()
    }

    fun getFilms() {
        viewModelScope.launch {
            if(page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE))
                return@launch

            pagingState = when(page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            val result = when(currentFilterSelected) {
                FilmType.MOVIE -> tmdbRepository.discoverFilms(
                    mediaType = currentFilterSelected.type,
                    page = page,
                    withCompanies = listOf(args.item.id),
                    sortBy = SortOptions.POPULARITY
                )
                FilmType.TV_SHOW -> tmdbRepository.discoverFilms(
                    mediaType = currentFilterSelected.type,
                    page = page,
                    withNetworks = listOf(args.item.id),
                    sortBy = SortOptions.POPULARITY
                )
            }

            when(result) {
                is Resource.Failure -> {
                    pagingState = when(page) {
                        1 -> PagingState.ERROR
                        else -> PagingState.PAGINATING_EXHAUST
                    }
                }
                Resource.Loading -> Unit
                is Resource.Success -> {
                    maxPage = result.data!!.totalPages
                    canPaginate = result.data.results.size == 20 || page < maxPage

                    if(page == 1) {
                        films.clear()
                    }

                    films.addAll(result.data.results)

                    pagingState = PagingState.IDLE

                    if(canPaginate)
                        page++
                }
            }
        }
    }

    fun onFilterChange(filter: FilmType) {
        if(onChangeFilterJob?.isActive == true)
            return

        onChangeFilterJob = viewModelScope.launch {
            currentFilterSelected = filter

            // Reset paging
            page = 1
            maxPage = 1
            films.clear()

            // Reload films
            getFilms()
        }
    }

    fun resetPagingState() {
        pagingState = PagingState.IDLE
    }
}