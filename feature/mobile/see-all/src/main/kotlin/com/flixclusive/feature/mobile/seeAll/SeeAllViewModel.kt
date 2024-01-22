package com.flixclusive.feature.mobile.seeAll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.FilmType.Companion.toFilmType
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.configuration.CategoryItem
import com.flixclusive.model.tmdb.TMDBSearchItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

internal fun String.getTypeFromQuery(): String {
    return if(contains("tv?")) "tv" else "movie"
}
@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    savedStateHandle: SavedStateHandle,
    appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

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
                    result.data?.run {
                        maxPage = totalPages
                        canPaginate = results.size == 20 || page < maxPage

                        if(page == 1) {
                            films.clear()
                        }

                        films.addAll(results)

                        pagingState = PagingState.IDLE

                        if(canPaginate)
                            this@SeeAllViewModel.page++
                    }
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