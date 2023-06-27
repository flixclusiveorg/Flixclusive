package com.flixclusive.presentation.recently_watched

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.flixclusive.presentation.appDestination
import com.flixclusive.presentation.destinations.HomeScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenDestination
import com.flixclusive.presentation.destinations.WatchlistScreenDestination
import com.ramcosta.composedestinations.spec.DestinationStyleAnimated

@OptIn(ExperimentalAnimationApi::class)
object RecentlyWatchedScreenTransition : DestinationStyleAnimated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition? {
        return when(initialState.appDestination()) {
            HomeScreenDestination, SearchScreenDestination -> slideInHorizontally { 1000 } + fadeIn()
            WatchlistScreenDestination -> slideInHorizontally { -1000 } + fadeIn()
            else -> null
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition? {
        return when(targetState.appDestination()) {
            HomeScreenDestination, SearchScreenDestination -> fadeOut() + slideOutHorizontally { 1000 }
            WatchlistScreenDestination -> fadeOut() + slideOutHorizontally { -1000 }
            else -> null
        }
    }
}