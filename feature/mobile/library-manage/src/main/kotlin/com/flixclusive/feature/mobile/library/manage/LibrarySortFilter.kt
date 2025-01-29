package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.locale.UiText
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR

internal sealed interface LibrarySortFilter {
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

    data object ItemCount : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.item_count)
    }

    companion object {
        val defaultFilters =
            persistentListOf(
                ModifiedAt,
                AddedAt,
                Name,
                ItemCount,
            )
    }

    enum class Direction {
        ASC,
        DESC,
        ;

        fun toggle() = if (this == ASC) DESC else ASC
        val isAscending get() = this == ASC
    }
}

internal data class FilterWithDirection(
    val filter: LibrarySortFilter,
    val direction: LibrarySortFilter.Direction
)
