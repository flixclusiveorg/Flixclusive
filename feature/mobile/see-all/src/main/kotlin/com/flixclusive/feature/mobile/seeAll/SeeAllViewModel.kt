package com.flixclusive.feature.mobile.seeAll

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.domain.catalog.usecase.PaginateItemsUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.SearchResponseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class SeeAllViewModel @Inject constructor(
    private val paginateItems: PaginateItemsUseCase,
    savedStateHandle: SavedStateHandle,
    dataStoreManager: DataStoreManager,
) : ViewModel() {
    private val navArgs = savedStateHandle.navArgs<SeeAllScreenNavArgs>()

    private var paginatingJob: Job? = null

    var items by mutableStateOf(persistentSetOf<Film>())
        private set

    private val _uiState = MutableStateFlow(SeeAllUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val showFilmTitles = dataStoreManager
        .getUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .map { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    init {
        paginate()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSearch(state: Boolean) {
        _uiState.update { it.copy(isSearching = state) }
    }

    fun paginate() {
        if (paginatingJob?.isActive == true) return

        paginatingJob = viewModelScope.launch {
            if (isDonePaginating()) return@launch

            _uiState.update {
                it.copy(pagingState = PagingDataState.Loading)
            }

            when (
                val result = paginateItems(
                    catalog = navArgs.catalog,
                    page = _uiState.value.page,
                )
            ) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    val data = result.data ?: SearchResponseData(
                        page = 1,
                        totalPages = 1,
                        hasNextPage = false,
                        results = emptyList(),
                    )
                    val canPaginate = data.results.size == 20 || data.page < data.totalPages

                    if (data.page == 1) {
                        items = items.clear()
                    }

                    items = items.addAll(data.results)

                    _uiState.update {
                        it.copy(
                            page = it.page + 1,
                            maxPage = data.totalPages,
                            canPaginate = canPaginate,
                            pagingState = PagingDataState.Success(isExhausted = !canPaginate),
                        )
                    }
                }

                is Resource.Failure -> {
                    val errorMessage = result.error ?: UiText.from(LocaleR.string.failed_to_paginate_items)
                    _uiState.update {
                        it.copy(
                            pagingState = when (it.page) {
                                1 -> PagingDataState.Error(errorMessage)
                                else -> PagingDataState.Success(isExhausted = true)
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks if pagination should stop based on current state.
     *
     * Returns true if:
     * - The current page is not the first page AND
     *   - Pagination is not allowed OR
     *   - The paging state is idle (indicating no more data to load)
     * - OR the search query is empty.
     * */
    private fun isDonePaginating(): Boolean =
        _uiState.value.let {
            (it.page != 1 && (!it.canPaginate || it.pagingState.isDone))
        }
}

@Immutable
internal data class SeeAllUiState(
    val pagingState: PagingDataState = PagingDataState.Loading,
    val page: Int = 1,
    val maxPage: Int = 1,
    val canPaginate: Boolean = false,
    val isSearching: Boolean = false,
)
