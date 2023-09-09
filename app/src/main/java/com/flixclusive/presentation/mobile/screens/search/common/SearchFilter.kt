package com.flixclusive.presentation.mobile.screens.search.common

import androidx.annotation.StringRes
import com.flixclusive.R


enum class SearchFilter(
    val type: String,
    @StringRes val resId: Int
) {
    ALL(
        type = "multi",
        resId = R.string.all
    ),
    MOVIE(
        type = "movie",
        resId = R.string.movies
    ),
    TV_SHOW(
        type = "tv",
        resId = R.string.tv_shows
    );
}