package com.flixclusive.feature.mobile.library.common.util

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R as LocaleR

interface LibrarySortFilter {
    val displayName: UiText

    data object ModifiedAt : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.modified_at)
    }

    data object AddedAt : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.added_at)
    }

    data object Name : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.name)
    }
}

enum class LibraryFilterDirection {
    ASC,
    DESC,
    ;

    fun toggle() = if (this == ASC) DESC else ASC

    val isAscending get() = this == ASC
}

data class FilterWithDirection(
    val filter: LibrarySortFilter,
    val direction: LibraryFilterDirection,
    val searchQuery: String,
)
