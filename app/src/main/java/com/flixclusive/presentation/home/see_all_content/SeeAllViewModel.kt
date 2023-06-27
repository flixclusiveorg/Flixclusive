package com.flixclusive.presentation.home.see_all_content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.GENRES_LIST
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.WatchProvider
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.usecase.POPULAR_MOVIE_FLAG
import com.flixclusive.domain.usecase.POPULAR_TV_FLAG
import com.flixclusive.domain.usecase.TOP_MOVIE_FLAG
import com.flixclusive.domain.usecase.TOP_TV_FLAG
import com.flixclusive.domain.usecase.TRENDING_FLAG
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.destinations.SeeAllScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = SeeAllScreenDestination.argsFrom(savedStateHandle)
    val films = mutableStateListOf<TMDBSearchItem>()

    private val genre = GENRES_LIST.find { args.flag.equals(it.name, true) }
    private val isItGenre = genre != null

    private val watchProvider = WatchProvider.values().find { it.id.toString() == args.flag }
    private val isItWatchProvider = watchProvider != null

    private var page by mutableStateOf(1)
    private var maxPage by mutableStateOf(1)
    var currentFilterSelected by mutableStateOf(
        when(args.flag) {
            TOP_TV_FLAG, POPULAR_TV_FLAG -> FilmType.TV_SHOW
            POPULAR_MOVIE_FLAG, TOP_MOVIE_FLAG, TRENDING_FLAG -> FilmType.MOVIE
            else -> {
                if(isItGenre) {
                    FilmType.MOVIE
                } else if(isItWatchProvider && watchProvider!!.isCompany) {
                    FilmType.MOVIE
                } else if(isItWatchProvider) {
                    FilmType.TV_SHOW
                } else {
                    throw IllegalStateException("A valid flag was not supplemented!")
                }
            }
        }
    )
        private set

    var canPaginate by mutableStateOf(false)
        private set
    var pagingState by mutableStateOf(PagingState.IDLE)
        private set

    init {
        getFilms()
    }

    fun resetPagingState() {
        pagingState = PagingState.IDLE
    }

    fun getFilms() {
        viewModelScope.launch {
            if(page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE))
                return@launch

            pagingState = when(page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            when(
                val result = getFilmsFromApiBasedOnFlag(
                    flag = args.flag,
                    filmType = currentFilterSelected,
                    page = page
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
        if(currentFilterSelected == filter)
            return

        currentFilterSelected = filter

        // Reset paging
        page = 1
        maxPage = 1
        films.clear()

        // Reload films
        getFilms()
    }

    private suspend fun getFilmsFromApiBasedOnFlag(
        flag: String,
        filmType: FilmType,
        page: Int,
    ): Resource<TMDBPageResponse<TMDBSearchItem>> {
       return when(flag) {
           TRENDING_FLAG, TOP_MOVIE_FLAG, TOP_TV_FLAG -> tmdbRepository.getTrending(
                page = page,
                mediaType = filmType.type
            )
            POPULAR_MOVIE_FLAG, POPULAR_TV_FLAG -> tmdbRepository.discoverFilms(
                page = page,
                mediaType = filmType.type
            )
            else -> {
                if(isItGenre) {
                    return tmdbRepository.discoverFilms(
                        mediaType = filmType.type,
                        page = page,
                        withGenres = listOf(genre!!),
                        sortBy = SortOptions.POPULARITY
                    )
                } else if(isItWatchProvider) {
                    return when(watchProvider!!.isCompany) {
                        true -> tmdbRepository.discoverFilms(
                            mediaType = filmType.type,
                            page = page,
                            withCompanies = listOf(watchProvider.id),
                            sortBy = SortOptions.POPULARITY
                        )
                        false -> tmdbRepository.discoverFilms(
                            mediaType = filmType.type,
                            page = page,
                            withNetworks = listOf(watchProvider.id),
                            sortBy = SortOptions.POPULARITY
                        )
                    }
                }

                throw IllegalStateException("A valid flag was not supplemented!")
            }
        }
    }
}
