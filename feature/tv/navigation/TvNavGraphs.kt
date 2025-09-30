package com.flixclusive.tv

import com.flixclusive.ROOT
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.flixclusive.feature.tv.film.destinations.FilmScreenDestination
import com.flixclusive.feature.tv.home.destinations.HomeScreenDestination
import com.flixclusive.feature.tv.search.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.dynamic.routedIn
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal object TvNavGraphs {

    val home = object : NavGraphSpec {
        override val route = "home"

        override val startRoute = HomeScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            HomeScreenDestination,
            FilmScreenDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val search = object : NavGraphSpec {
        override val route = "search"

        override val startRoute = SearchScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            SearchScreenDestination,
            FilmScreenDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val root = object : NavGraphSpec {
        override val route = ROOT

        override val startRoute = SplashScreenDestination

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            SplashScreenDestination,
        ).associateBy { it.route }

        override val nestedNavGraphs = listOf(
            home,
            search
        )
    }
}