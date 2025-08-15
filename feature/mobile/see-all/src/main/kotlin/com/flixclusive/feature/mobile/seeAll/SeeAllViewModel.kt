package com.flixclusive.feature.mobile.seeAll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.pagination.PagingState
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.SeeAllScreenNavArgs
import com.flixclusive.domain.catalog.CatalogItemsProviderUseCase
import com.flixclusive.model.configuration.catalog.HomeCatalog
import com.flixclusive.model.configuration.catalog.SearchCatalog
import com.flixclusive.model.datastore.user.UiPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.FilmType.Companion.toFilmType
import com.flixclusive.model.film.util.replaceTypeInUrl
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.DEFAULT_CATALOG_MEDIA_TYPE
import com.flixclusive.model.provider.ProviderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SeeAllViewModel @Inject constructor(
    private val catalogItemsProviderUseCase: CatalogItemsProviderUseCase,
    savedStateHandle: SavedStateHandle,
    dataStoreManager: DataStoreManager,
) : ViewModel() {
    val uiPreferences = dataStoreManager
        .getUserPrefs<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .asStateFlow(viewModelScope)

    private val args = savedStateHandle.navArgs<SeeAllScreenNavArgs>()
    var catalog: Catalog = args.item
    val films = mutableStateListOf<FilmSearchItem>()
    val isMediaTypeDefault = catalog.mediaType == DEFAULT_CATALOG_MEDIA_TYPE
    val isProviderCatalog = catalog is ProviderCatalog

    private var page by mutableIntStateOf(1)
    private var maxPage by mutableIntStateOf(1)
    var currentFilterSelected by mutableStateOf(
        when {
            isMediaTypeDefault -> catalog.url.type
            else -> catalog.mediaType.toFilmType()
        }
    )
        private set

    var canPaginate by mutableStateOf(false)
        private set
    var pagingState by mutableStateOf(com.flixclusive.core.common.pagination.PagingState.IDLE)
        private set

    init {
        getFilms()
    }

    fun resetPagingState() {
        pagingState = com.flixclusive.core.common.pagination.PagingState.IDLE
    }

    // TODO: Add job checking here
    fun getFilms() {
        viewModelScope.launch {
            if (hasNextPage) return@launch

            pagingState = when (page) {
                1 -> com.flixclusive.core.common.pagination.PagingState.LOADING
                else -> com.flixclusive.core.common.pagination.PagingState.PAGINATING
            }

            val catalog = catalog.parseCorrectTmdbUrl()

            when (
                val result = catalogItemsProviderUseCase(
                    catalog = catalog,
                    page = page
                )
            ) {
                is Resource.Failure -> {
                    pagingState = when (page) {
                        1 -> com.flixclusive.core.common.pagination.PagingState.ERROR
                        else -> com.flixclusive.core.common.pagination.PagingState.EXHAUSTED
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

                        pagingState = com.flixclusive.core.common.pagination.PagingState.IDLE

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
        get() = page != 1 && (page == 1 || !canPaginate || pagingState != com.flixclusive.core.common.pagination.PagingState.IDLE)

    private fun Catalog.getCorrectQuery() = url.replace("all/", "${currentFilterSelected.type}/")

    private val String.type: FilmType
        get() = if (contains("tv?")) FilmType.TV_SHOW else FilmType.MOVIE

    private fun Catalog.parseCorrectTmdbUrl(): Catalog {
        if (this is ProviderCatalog)
            return this

        val correctUrl = when {
            isMediaTypeDefault && url.contains("all/") -> getCorrectQuery()
            isMediaTypeDefault -> url.replaceTypeInUrl(currentFilterSelected.type)
            else -> url
        }

        return when (this) {
            is HomeCatalog -> copy(url = correctUrl)
            is SearchCatalog -> copy(url = correctUrl)
            else -> this
        }
    }
}
