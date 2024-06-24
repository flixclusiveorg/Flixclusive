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
import com.flixclusive.core.util.film.replaceTypeInUrl
import com.flixclusive.domain.category.CategoryItemsProviderUseCase
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.category.Category
import com.flixclusive.model.tmdb.category.DEFAULT_CATEGORY_MEDIA_TYPE
import com.flixclusive.model.tmdb.category.HomeCategory
import com.flixclusive.model.tmdb.category.SearchCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val categoryItemsProviderUseCase: CategoryItemsProviderUseCase,
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
    var category: Category = args.item
    val films = mutableStateListOf<FilmSearchItem>()
    val isMediaTypeDefault = category.mediaType == DEFAULT_CATEGORY_MEDIA_TYPE
    val isProviderCatalog = category is ProviderCatalog

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(
        when {
            isMediaTypeDefault -> category.url.type
            else -> category.mediaType.toFilmType()
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
            if (hasNextPage) return@launch

            pagingState = when (page) {
                1 -> PagingState.LOADING
                else -> PagingState.PAGINATING
            }

            val category = category.parseCorrectTmdbUrl()

            when (
                val result = categoryItemsProviderUseCase(
                    category = category,
                    page = page
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
                        canPaginate = results.size == 20 || page < maxPage || hasNextPage

                        if (page == 1) {
                            films.clear()
                        }

                        films.addAll(results)

                        pagingState = PagingState.IDLE

                        if (canPaginate)
                            this@SeeAllViewModel.page++
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


    private val hasNextPage: Boolean
        get() = page != 1 && (page == 1 || !canPaginate || pagingState != PagingState.IDLE)

    private fun Category.getCorrectQuery() = url.replace("all/", "${currentFilterSelected.type}/")

    private val String.type: FilmType
        get() = if (contains("tv?")) FilmType.TV_SHOW else FilmType.MOVIE

    private fun Category.parseCorrectTmdbUrl(): Category {
        if (this is ProviderCatalog)
            return this

        val correctUrl = when {
            isMediaTypeDefault && url.contains("all/") -> getCorrectQuery()
            isMediaTypeDefault -> url.replaceTypeInUrl(currentFilterSelected.type)
            else -> url
        }

        return when (this) {
            is HomeCategory -> copy(url = correctUrl)
            is SearchCategory -> copy(url = correctUrl)
            else -> this
        }
    }
}