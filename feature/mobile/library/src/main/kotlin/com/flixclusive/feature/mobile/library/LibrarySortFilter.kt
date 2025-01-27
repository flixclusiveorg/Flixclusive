package com.flixclusive.feature.mobile.library

import com.flixclusive.core.locale.UiText
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR

sealed interface LibrarySortFilter {
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

    data object Rating : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.rating)
    }

    data object ReleaseDate : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.release_date)
    }

    companion object {
        val defaultFilters =
            persistentListOf(
                ModifiedAt,
                AddedAt,
                Name,
                Rating,
                ReleaseDate,
            )
    }

    enum class Direction {
        ASC,
        DESC,
        ;

        fun toggle() = if (this == ASC) DESC else ASC
    }
}
