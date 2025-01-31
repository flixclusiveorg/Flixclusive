package com.flixclusive.feature.mobile.library.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.flixclusive.core.locale.UiText
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.model.film.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import java.util.Date
import javax.inject.Inject
import com.flixclusive.core.locale.R as LocaleR

@HiltViewModel
internal class LibraryDetailsViewModel @Inject constructor(

) : ViewModel() {

}

@Immutable
internal data class LibraryDetailsUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorApiMessage: String? = null,
    val isShowingFilterSheet: Boolean = false,
    val isShowingSearchBar: Boolean = false,
    val isMultiSelecting: Boolean = false,
    val longClickedFilm: Film? = null,
    val selectedFilter: LibrarySortFilter = LibrarySortFilter.AddedAt,
    val selectedFilterDirection: LibraryFilterDirection = LibraryFilterDirection.ASC,
    val selectedItems: Set<Film> = emptySet(),
)

@Immutable
internal data class FilmWithAddedTime(
    val film: Film,
    val addedAt: Date
) {
    companion object {
        fun from(film: Film, addedAt: Date): FilmWithAddedTime {
            return FilmWithAddedTime(
                film = film,
                addedAt = addedAt
            )
        }
    }
}

internal object LibraryDetailsFilters {
    data object Year : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.year)
    }

    data object Rating : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.rating)
    }

    val defaultFilters = persistentListOf(
        LibrarySortFilter.AddedAt,
        LibrarySortFilter.Name,
        Year,
        Rating,
    )
}
