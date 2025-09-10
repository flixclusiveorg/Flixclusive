package com.flixclusive.feature.mobile.film

import androidx.compose.runtime.Immutable
import com.flixclusive.feature.mobile.film.component.ContentTabs
import com.flixclusive.core.strings.R as LocaleR

/**
 * Different types of tabs that can be displayed in the [ContentTabs] component.
 */
@Immutable
internal enum class ContentTabType(val stringId: Int) {
    Episodes(LocaleR.string.episodes),
    MoreLikeThis(R.string.more_like_this),
    Collections(LocaleR.string.collections);

    val isOnMoreLikeThisSection: Boolean
        get() = this == MoreLikeThis

    val isOnCollectionsSection: Boolean
        get() = this == Collections

    val isOnEpisodesSection: Boolean
        get() = this == Episodes

    val isOnFilmsSection: Boolean
        get() = isOnCollectionsSection || isOnMoreLikeThisSection
}
