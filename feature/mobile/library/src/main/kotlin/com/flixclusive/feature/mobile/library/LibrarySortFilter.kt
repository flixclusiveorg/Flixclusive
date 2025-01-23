package com.flixclusive.feature.mobile.library

import com.flixclusive.core.locale.UiText
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR

sealed interface LibrarySortFilter {
    val direction: Direction
    val displayName: UiText

    data class ModifiedAt(
        override val direction: Direction,
    ) : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.modified_at)
    }

    data class AddedAt(
        override val direction: Direction,
    ) : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.added_at)
    }

    data class Name(
        override val direction: Direction,
    ) : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.name)
    }

    data class Rating(
        override val direction: Direction,
    ) : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.rating)
    }

    data class ReleaseDate(
        override val direction: Direction,
    ) : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.release_date)
    }

    enum class Direction {
        ASC,
        DESC,
        ;

        fun toggle() = if (this == ASC) DESC else ASC
    }


    companion object {
        val defaultFilters =
            persistentListOf(
                ModifiedAt(Direction.DESC),
                AddedAt(Direction.DESC),
                Name(Direction.DESC),
                Rating(Direction.DESC),
                ReleaseDate(Direction.DESC),
            )
    }
}
