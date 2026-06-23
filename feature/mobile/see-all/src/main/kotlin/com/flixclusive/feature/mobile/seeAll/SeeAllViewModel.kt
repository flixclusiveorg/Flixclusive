package com.flixclusive.feature.mobile.seeAll

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.domain.catalog.usecase.GetCatalogItemsUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.provider.Catalog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel(assistedFactory = SeeAllViewModel.Factory::class)
class SeeAllViewModel @AssistedInject constructor(
    private val getCatalogItems: GetCatalogItemsUseCase,
    dataStoreManager: DataStoreManager,
    @Assisted private val navArgs: Catalog,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navArgs: Catalog): SeeAllViewModel
    }

    private val seenIds = hashSetOf<String>()
    private var paginatingJob: Job? = null

    val items = mutableStateListOf<MediaMetadata>()

    private val _uiState = MutableStateFlow(SeeAllUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val showMediaTitles = dataStoreManager
        .getUserPrefsAsFlow<UiPreferences>(UserPreferences.UI_PREFS_KEY)
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

            val page = _uiState.value.page
            getCatalogItems(catalog = navArgs, page = page).collect { response ->
                when (response) {
                    Async.Loading -> {
                        _uiState.update { it.copy(pagingState = PagingState.Loading) }
                    }

                    is Async.Failure -> {
                        _uiState.update {
                            it.copy(
                                pagingState = when (page) {
                                    1 -> PagingState.Error(UiText.from(LocaleR.string.failed_to_paginate_items))
                                    else -> PagingState.Exhausted
                                },
                            )
                        }
                    }

                    is Async.Success -> {
                        val data = response.data
                        val canPaginate = data.results.size == 20 || data.page < data.totalPages

                        if (data.page == 1) {
                            items.clear()
                        }

                        items.addAll(
                            data.results
                                .fastFilter { seenIds.add(it.id) }
                        )

                        val pagingState = when {
                            canPaginate -> PagingState.Idle
                            else -> PagingState.Exhausted
                        }

                        _uiState.update {
                            it.copy(
                                page = it.page + 1,
                                maxPage = data.totalPages,
                                canPaginate = canPaginate,
                                pagingState = pagingState,
                            )
                        }
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
            (it.page != 1 && (!it.canPaginate || it.pagingState.isExhausted))
        }
}

@Immutable
data class SeeAllUiState(
    val pagingState: PagingState = PagingState.Loading,
    val page: Int = 1,
    val maxPage: Int = 1,
    val canPaginate: Boolean = false,
    val isSearching: Boolean = false,
)
