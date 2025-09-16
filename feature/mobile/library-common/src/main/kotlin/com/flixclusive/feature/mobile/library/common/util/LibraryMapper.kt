package com.flixclusive.feature.mobile.library.common.util

import android.content.Context
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.core.strings.R as LocaleR

object LibraryMapper {
    private fun WatchProgressWithMetadata.toLibraryListItem() =
        LibraryListItemWithMetadata(
            metadata = film,
            item = LibraryListItem(
                id = id,
                filmId = filmId,
                listId = LibraryListUtil.WATCH_PROGRESS_LIB_ID,
            ),
        )

    private fun WatchlistWithMetadata.toLibraryListItem() =
        LibraryListItemWithMetadata(
            metadata = film,
            item = LibraryListItem(
                id = id,
                filmId = filmId,
                listId = LibraryListUtil.WATCHLIST_LIB_ID,
            ),
        )

    fun List<WatchProgressWithMetadata>.toWatchProgressLibraryList(context: Context) =
        LibraryListWithItems(
            list = LibraryList(
                id = LibraryListUtil.WATCH_PROGRESS_LIB_ID,
                ownerId = -1,
                name = context.getString(LocaleR.string.recently_watched),
                description = context.getString(LocaleR.string.recently_watched_description),
            ),
            items = fastMap { it.toLibraryListItem() }
        )

    fun List<WatchlistWithMetadata>.toWatchlistLibraryList(context: Context) =
        LibraryListWithItems(
            list = LibraryList(
                id = LibraryListUtil.WATCHLIST_LIB_ID,
                ownerId = -1,
                name = context.getString(LocaleR.string.watchlist),
                description = context.getString(LocaleR.string.watchlist_description),
            ),
            items = fastMap { it.toLibraryListItem() }
        )
}
