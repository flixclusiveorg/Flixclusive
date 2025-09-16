@file:Suppress("ktlint:standard:filename")

package com.flixclusive.feature.mobile.library.manage.util

import com.flixclusive.core.database.entity.DBFilmItem
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.WatchHistoryItem
import com.flixclusive.core.database.entity.watchlist.WatchlistItem
import com.flixclusive.core.strings.UiText
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.manage.EmphasisLibraryList
import com.flixclusive.feature.mobile.library.manage.ItemCount
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.UiLibraryList
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

internal fun List<UiLibraryList>.filter(libraryFilter: LibraryFilter): List<UiLibraryList> {
    val (filter, direction, query) = libraryFilter

    val queriedList = if (query.isNotBlank()) {
        filter { searchInLibrary(it, query) }
    } else {
        this
    }

    return queriedList.sortedWith(
        compareBy<UiLibraryList>(
            selector = {
                val item = it.mapToListPreview()

                when (filter) {
                    LibrarySortFilter.Name -> item.list.name
                    LibrarySortFilter.AddedAt -> item.list.createdAt.time
                    LibrarySortFilter.ModifiedAt -> item.list.updatedAt.time
                    ItemCount -> item.itemsCount
                    else -> throw IllegalStateException(
                        "Library filter [${filter}] is not recognized",
                    )
                }
            },
        ).let { comparator ->
            if (direction == LibraryFilterDirection.ASC) {
                comparator
            } else {
                comparator.reversed()
            }
        },
    )
}

private fun searchInLibrary(library: UiLibraryList, query: String): Boolean {
    return when (library) {
        is LibraryListWithPreview -> {
            if (library.list.name.contains(query, true)) {
                return true
            }

            library.list.description?.let { description ->
                if (description.contains(query, true)) {
                    return true
                }
            }

            false
        }

        is EmphasisLibraryList -> {
            if (library.library.list.name.contains(query, true)) {
                return true
            }

            library.library.list.description?.let { description ->
                if (description.contains(query, true)) {
                    return true
                }
            }

            false
        }
    }
}

internal fun UiLibraryList.mapToListPreview(): LibraryListWithPreview {
    if (this is LibraryListWithPreview) return this

    return (this as EmphasisLibraryList).library
}
