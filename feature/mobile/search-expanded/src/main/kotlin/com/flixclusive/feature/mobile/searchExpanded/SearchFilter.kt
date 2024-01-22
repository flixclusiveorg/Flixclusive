package com.flixclusive.feature.mobile.searchExpanded

import androidx.annotation.StringRes
import com.flixclusive.core.util.R as UtilR


enum class SearchFilter(
    val type: String,
    @StringRes val resId: Int
) {
    ALL(
        type = "multi",
        resId = UtilR.string.all
    ),
    MOVIE(
        type = "movie",
        resId = UtilR.string.movies
    ),
    TV_SHOW(
        type = "tv",
        resId = UtilR.string.tv_shows
    );
}