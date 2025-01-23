package com.flixclusive.feature.mobile.library

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
        var newFilter = filter

        val isUpdatingDirection = _uiState.value.selectedFilter == filter
        if (isUpdatingDirection) {
            newFilter = getNewDirection(filter)
        }

        _uiState.update { it.copy(selectedFilter = newFilter) }
    }

    private fun getNewDirection(filter: LibrarySortFilter): LibrarySortFilter {
        return when (filter) {
            is LibrarySortFilter.AddedAt -> filter.copy(direction = filter.direction.toggle())
            is LibrarySortFilter.ModifiedAt -> filter.copy(direction = filter.direction.toggle())
            is LibrarySortFilter.Name -> filter.copy(direction = filter.direction.toggle())
            is LibrarySortFilter.Rating -> filter.copy(direction = filter.direction.toggle())
            is LibrarySortFilter.ReleaseDate -> filter.copy(direction = filter.direction.toggle())
        }
    }
}

data class LibraryUiState(
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.ModifiedAt(LibrarySortFilter.Direction.DESC),
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
