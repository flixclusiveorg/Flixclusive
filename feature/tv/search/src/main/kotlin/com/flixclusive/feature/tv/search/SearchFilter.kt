package com.flixclusive.feature.tv.search

import androidx.annotation.StringRes
import com.flixclusive.core.locale.R as LocaleR


@Deprecated("Use SearchFilterSet")
enum class SearchFilter(
    val type: String,
    @StringRes val resId: Int
) {
    ALL(
        type = "multi",
        resId = LocaleR.string.all
    ),
    MOVIE(
        type = "movie",
        resId = LocaleR.string.movie
    ),
    TV_SHOW(
        type = "tv",
        resId = LocaleR.string.tv_show
    );
}