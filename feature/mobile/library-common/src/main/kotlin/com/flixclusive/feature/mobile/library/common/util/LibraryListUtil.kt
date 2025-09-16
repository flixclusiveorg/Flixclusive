package com.flixclusive.feature.mobile.library.common.util

import com.flixclusive.core.database.entity.library.LibraryList

object LibraryListUtil {
    const val WATCHLIST_LIB_ID = -1
    const val WATCH_PROGRESS_LIB_ID = -2

    const val WATCHLIST_SEARCHABLE_NAME = "watchlist"
    const val WATCH_PROGRESS_SEARCHABLE_NAME = "recently watched"

    val LibraryList.isCustom: Boolean get() = id > 0
}
