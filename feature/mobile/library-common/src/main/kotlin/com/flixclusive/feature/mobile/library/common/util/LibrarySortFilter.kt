package com.flixclusive.feature.mobile.library.common.util

import androidx.compose.runtime.Immutable
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R as LocaleR

interface LibrarySortFilter {
    val displayName: UiText

    @Immutable
    data object ModifiedAt : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.modified_at)
    }

    @Immutable
    data object AddedAt : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.added_at)
    }

    @Immutable
    data object Name : LibrarySortFilter {
        override val displayName = UiText.from(LocaleR.string.name)
    }
}
