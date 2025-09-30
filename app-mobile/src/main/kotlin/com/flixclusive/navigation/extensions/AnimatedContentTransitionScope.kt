package com.flixclusive.navigation.extensions

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph

internal val NavDestination.hostNavGraph: NavGraph
    get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
internal fun AnimatedContentTransitionScope<*>.defaultEnterTransition(
    isTv: Boolean,
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): EnterTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id || isTv) {
        return fadeIn()
    }

    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
internal fun AnimatedContentTransitionScope<*>.defaultExitTransition(
    isTv: Boolean,
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): ExitTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id || isTv) {
        return fadeOut()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
internal fun AnimatedContentTransitionScope<*>.defaultPopEnterTransition(isTv: Boolean): EnterTransition {
    if (isTv) {
        return fadeIn()
    }

    return fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End)
}

@ExperimentalAnimationApi
internal fun AnimatedContentTransitionScope<*>.defaultPopExitTransition(isTv: Boolean): ExitTransition {
    if (isTv) {
        return fadeOut()
    }

    return fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
}

