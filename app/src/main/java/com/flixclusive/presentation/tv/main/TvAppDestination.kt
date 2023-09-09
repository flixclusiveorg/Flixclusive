package com.flixclusive.presentation.tv.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flixclusive.R
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.destinations.HomeTvScreenDestination
import com.flixclusive.presentation.destinations.SearchTvScreenDestination
import com.flixclusive.presentation.destinations.WatchlistTvScreenDestination
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec

enum class TvAppDestination(
    val direction: Direction,
    val navGraph: NavGraphSpec,
    @DrawableRes val iconSelected: Int,
    @DrawableRes val iconUnselected: Int,
    @StringRes val label: Int
) {
    Home(
        direction = HomeTvScreenDestination(),
        navGraph = NavGraphs.tvRoot,
        iconSelected = R.drawable.home,
        iconUnselected = R.drawable.home_outlined,
        label = R.string.home
    ),
    Search(
        direction = SearchTvScreenDestination,
        navGraph = NavGraphs.tvRoot,
        iconSelected = R.drawable.search,
        iconUnselected = R.drawable.search_outlined,
        label = R.string.search
    ),
    Watchlist(
        direction = WatchlistTvScreenDestination,
        navGraph = NavGraphs.tvRoot,
        iconSelected = R.drawable.bookmark,
        iconUnselected = R.drawable.bookmark_outlined,
        label = R.string.watchlist
    )
}