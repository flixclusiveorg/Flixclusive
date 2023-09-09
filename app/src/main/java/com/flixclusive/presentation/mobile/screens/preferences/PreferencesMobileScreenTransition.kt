package com.flixclusive.presentation.mobile.screens.preferences

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
import com.flixclusive.presentation.destinations.SearchMobileScreenDestination
import com.flixclusive.presentation.destinations.WatchlistMobileScreenDestination
import com.ramcosta.composedestinations.spec.DestinationStyleAnimated

@OptIn(ExperimentalAnimationApi::class)
object PreferencesMobileScreenTransition : DestinationStyleAnimated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition? {
        return when(initialState.appDestination()) {
            SearchMobileScreenDestination, WatchlistMobileScreenDestination -> slideInHorizontally { 1000 } + fadeIn()
            else -> null
        }
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition? {
        return when(targetState.appDestination()) {
            SearchMobileScreenDestination, WatchlistMobileScreenDestination -> fadeOut() + slideOutHorizontally { 1000 }
            else -> null
        }
    }
}