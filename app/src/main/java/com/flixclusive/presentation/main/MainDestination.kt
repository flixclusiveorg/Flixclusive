package com.flixclusive.presentation.main

import androidx.annotation.StringRes
import com.flixclusive.R
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.destinations.HomeScreenDestination
import com.flixclusive.presentation.destinations.RecentlyWatchedScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenDestination
import com.flixclusive.presentation.destinations.WatchlistScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

enum class MainDestination(
    val direction: DirectionDestinationSpec,
    val navGraph: NavGraphSpec,
    val iconSelected: IconResource,
    val iconUnselected: IconResource,
    @StringRes val label: Int
) {
    Home(
        direction = HomeScreenDestination,
        navGraph = NavGraphs.home,
        iconSelected = IconResource.fromDrawableResource(R.drawable.home),
        iconUnselected = IconResource.fromDrawableResource(R.drawable.home_outlined),
        label = R.string.home
    ),
    Search(
        direction = SearchScreenDestination,
        navGraph = NavGraphs.search,
        iconSelected = IconResource.fromDrawableResource(R.drawable.search),
        iconUnselected = IconResource.fromDrawableResource(R.drawable.search_outlined),
        label = R.string.search
    ),
    Recent(
        direction = RecentlyWatchedScreenDestination,
        navGraph = NavGraphs.recent,
        iconSelected = IconResource.fromDrawableResource(R.drawable.time_circle),
        iconUnselected = IconResource.fromDrawableResource(R.drawable.time_circle_outlined),
        label = R.string.recently_watched
    ),
    Watchlist(
        direction = WatchlistScreenDestination,
        navGraph = NavGraphs.watchlist,
        iconSelected = IconResource.fromDrawableResource(R.drawable.bookmark),
        iconUnselected = IconResource.fromDrawableResource(R.drawable.bookmark_outlined),
        label = R.string.watchlist
    ),
}