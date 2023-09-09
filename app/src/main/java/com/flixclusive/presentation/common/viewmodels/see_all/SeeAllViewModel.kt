package com.flixclusive.presentation.common.viewmodels.see_all

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.config.CategoryItem
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.FilmType.Companion.toFilmType
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.navArgs
import com.flixclusive.presentation.utils.FormatterUtils.getTypeFromQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.navArgs<SeeAllScreenNavArgs>()
    val itemConfig: CategoryItem = args.item
    val films = mutableStateListOf<TMDBSearchItem>()
    val filmTypeCouldBeBoth = itemConfig.mediaType == "all"

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(
        if(itemConfig.mediaType == "all") {
            itemConfig.query.getTypeFromQuery().toFilmType()
        } else itemConfig.mediaType.toFilmType()
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

            val urlQuery = if(filmTypeCouldBeBoth && itemConfig.query.contains("all/")) {
                itemConfig.query.replace("all/", "${currentFilterSelected.type}/")
            } else if(filmTypeCouldBeBoth) {
                itemConfig.query.replaceTypeInUrl(currentFilterSelected.type)
            } else itemConfig.query

            when(
                val result = tmdbRepository.paginateConfigItems(
                    url = urlQuery,
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

    private fun String.replaceTypeInUrl(type: String): String {
        val pattern = Pattern.compile("(?<=/)[a-z]+(?=\\?)")
        val matcher = pattern.matcher(this)

        if (matcher.find()) {
            return matcher.replaceFirst(type)
        }

        return this
    }
}