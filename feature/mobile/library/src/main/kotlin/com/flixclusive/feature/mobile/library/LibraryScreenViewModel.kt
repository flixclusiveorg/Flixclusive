package com.flixclusive.feature.mobile.library

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class LibraryScreenViewModel @Inject constructor(

) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    fun updateFilter(filter: LibrarySortFilter) {
        val isUpdatingDirection = _uiState.value.selectedFilter == filter

        _uiState.update {
            if (isUpdatingDirection) {
                it.copy(filterDirection = it.filterDirection.toggle())
            } else {
                it.copy(selectedFilter = filter)
            }
        }
    }
}

@Immutable
internal data class LibraryUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.ModifiedAt,
    val filterDirection: LibrarySortFilter.Direction = LibrarySortFilter.Direction.ASC,
    val selectedLibraries: List<LibraryListWithPreview> = emptyList(),
)

internal data class LibraryListWithPreview(
    val list: LibraryList,
    val itemsCount: Int,
    val previews: List<PreviewPoster>
)

internal data class PreviewPoster(
    val title: String?,
    val posterPath: String?,
) {
    companion object {
        fun Film.toPreviewPoster(): PreviewPoster {
            return PreviewPoster(
                title = title,
                posterPath = posterImage
            )
        }
    }
}
