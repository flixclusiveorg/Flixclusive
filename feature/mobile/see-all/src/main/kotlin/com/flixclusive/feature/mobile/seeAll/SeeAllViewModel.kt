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
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navargs.SeeAllScreenNavArgs
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.domain.catalog.CatalogItemsProviderUseCase
import com.flixclusive.model.configuration.catalog.HomeCatalog
import com.flixclusive.model.configuration.catalog.SearchCatalog
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.FilmType.Companion.toFilmType
import com.flixclusive.model.film.util.replaceTypeInUrl
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.DEFAULT_CATALOG_MEDIA_TYPE
import com.flixclusive.model.provider.ProviderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SeeAllViewModel @Inject constructor(
    private val catalogItemsProviderUseCase: CatalogItemsProviderUseCase,
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

            val catalog = catalog.parseCorrectTmdbUrl()

            when (
                val result = catalogItemsProviderUseCase(
                    catalog = catalog,
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