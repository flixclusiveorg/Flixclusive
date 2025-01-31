@file:Suppress("ktlint:standard:filename")

package com.flixclusive.feature.mobile.library.manage.util

import com.flixclusive.core.locale.UiText
import com.flixclusive.feature.mobile.library.common.util.FilterWithDirection
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.manage.EmphasisLibraryList
import com.flixclusive.feature.mobile.library.manage.ItemCount
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.UiLibraryList
import com.flixclusive.model.database.DBFilmItem
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.WatchlistItem
import java.util.Date

internal fun List<DBFilmItem>.toUiLibraryList(
    id: Int,
    userId: Int,
    searchableName: String,
    name: UiText,
    description: UiText,
): EmphasisLibraryList {
    val previews = take(3).map { it.film.toPreviewPoster() }
    val lastUpdatedAt = firstOrNull().getLastUpdatedDate()

    return EmphasisLibraryList(
        name = name,
        description = description,
        library =
            LibraryListWithPreview(
                list =
                    LibraryList(
                        id = id,
                        ownerId = userId,
                        name = searchableName,
                        createdAt = Date(0),
                        updatedAt = lastUpdatedAt,
                    ),
                itemsCount = size,
                previews = previews,
            ),
    )
}

internal fun DBFilmItem?.getLastUpdatedDate(): Date {
    return when (this) {
        is WatchHistoryItem -> dateWatched
        is WatchlistItem -> addedOn
        else -> Date(0)
    }
}

internal fun List<UiLibraryList>.filter(filterWithDirection: FilterWithDirection): List<UiLibraryList> {
    return sortedWith(
        compareBy<UiLibraryList>(
            selector = {
                val item = it.mapToListPreview()

                when (filterWithDirection.filter) {
                    LibrarySortFilter.Name -> item.list.name
                    LibrarySortFilter.AddedAt -> item.list.createdAt.time
                    LibrarySortFilter.ModifiedAt -> item.list.updatedAt.time
                    ItemCount -> item.itemsCount
                    else -> throw IllegalStateException(
                        "Library filter [${filterWithDirection.filter}] is not recognized",
                    )
                }
            },
        ).let { comparator ->
            if (filterWithDirection.direction == LibraryFilterDirection.ASC) {
                comparator
            } else {
                comparator.reversed()
            }
        },
    )
}

internal fun UiLibraryList.mapToListPreview(): LibraryListWithPreview {
    if (this is LibraryListWithPreview) return this

    return (this as EmphasisLibraryList).library
}
