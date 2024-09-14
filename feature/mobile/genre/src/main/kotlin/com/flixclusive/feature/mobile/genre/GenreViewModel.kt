package com.flixclusive.feature.mobile.genre

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.GenreScreenNavArgs
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.data.tmdb.SortOptions
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.FilmType.Companion.toFilmType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class GenreViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    savedStateHandle: SavedStateHandle,
    appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedAppSettings
        )

    private val genreArgs = savedStateHandle.navArgs<GenreScreenNavArgs>()
    val filmTypeCouldBeBoth = genreArgs.genre.mediaType == "all"
    val films = mutableStateListOf<FilmSearchItem>()

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(
        if (filmTypeCouldBeBoth)
            FilmType.MOVIE
        else genreArgs.genre.mediaType!!.toFilmType()
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
            if (page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE))
                return@launch

            pagingState = when (page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            when (
                val result = tmdbRepository.discoverFilms(
                    mediaType = currentFilterSelected.type,
                    page = page,
                    withGenres = listOf(genreArgs.genre),
                    sortBy = SortOptions.POPULARITY
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
                        maxPage = totalPages
                        canPaginate = results.size == 20 || page < maxPage

                        if (page == 1) {
                            films.clear()
                        }

                        films.addAll(results)

                        pagingState = PagingState.IDLE

                        if (canPaginate)
                            this@GenreViewModel.page++
                    }
                }
            }
        }
    }

    fun onFilterChange(filter: FilmType) {
        if (currentFilterSelected == filter)
            return

        currentFilterSelected = filter

        // Reset paging
        page = 1
        maxPage = 1
        films.clear()

        // Reload films
        getFilms()
    }

    fun resetPagingState() {
        pagingState = PagingState.IDLE
    }
}