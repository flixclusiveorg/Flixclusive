package com.flixclusive.feature.tv.search

import androidx.annotation.StringRes
import com.flixclusive.core.util.R as UtilR


@Deprecated("Use SearchFilterSet")
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
        resId = UtilR.string.movie
    ),
    TV_SHOW(
        type = "tv",
        resId = UtilR.string.tv_show
    );
}