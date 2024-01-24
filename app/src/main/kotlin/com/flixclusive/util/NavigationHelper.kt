package com.flixclusive.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.flixclusive.ROOT
import com.flixclusive.mobile.MobileAppNavigator
import com.flixclusive.mobile.MobileNavGraphs
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.tv.TvAppNavigator
import com.flixclusive.tv.TvNavGraphs
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal fun NavHostController.navigateSingleTopTo(
    direction: NavGraphSpec,
    isPoppingToRoot: Boolean = false,
) = navigate(direction) {
    if (isPoppingToRoot) {
        popUpTo(direction.startRoute.route)
    } else {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }

    launchSingleTop = true
    restoreState = true
}

private fun NavBackStackEntry.lifecycleIsResumed() =
    lifecycle.currentState == Lifecycle.State.RESUMED

internal fun NavController.navigateIfResumed(direction: Direction) {
    if (currentBackStackEntry?.lifecycleIsResumed() == false) {
        return
    }

    navigate(direction)
}

internal fun NavDestination.navGraph(isTv: Boolean = false): NavGraphSpec {
    hierarchy.forEach { destination ->
        when (isTv) {
            true -> TvNavGraphs.root
            else -> MobileNavGraphs.root
        }.nestedNavGraphs.forEach { navGraph ->
            if (destination.route == navGraph.route) {
                return navGraph
            }
        }
    }

    throw RuntimeException("Unknown nav graph for destination $route")
}

@Stable
@Composable
internal fun NavController.currentScreenAsState(
    initialNavGraph: NavGraphSpec,
    isTv: Boolean = false
): State<NavGraphSpec> {
    val selectedItem = remember { mutableStateOf(initialNavGraph) }

    DisposableEffect(this) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val doesNotHaveNavGraph = destination.parent?.route == ROOT
            if (doesNotHaveNavGraph) {
                return@OnDestinationChangedListener
            }

            selectedItem.value = destination.navGraph(isTv = isTv)
        }

        addOnDestinationChangedListener(listener)

        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }

    return selectedItem
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    isTv: Boolean = false,
    closeApp: () -> Unit,
    previewFilm: (Film) -> Unit = {},
    play: (Film, TMDBEpisode?) -> Unit = { _, _ -> },
) {
    DestinationsNavHost(
        engine = rememberAnimatedNavHostEngine(
            rootDefaultAnimations = RootNavGraphDefaultAnimations(
                enterTransition = { defaultEnterTransition(initialState, targetState) },
                exitTransition = { defaultExitTransition(initialState, targetState) },
                popEnterTransition = { defaultPopEnterTransition() },
                popExitTransition = { defaultPopExitTransition() },
            )
        ),
        navController = navController,
        navGraph = if (isTv) TvNavGraphs.root else MobileNavGraphs.root,
        dependenciesContainerBuilder = {
            if (isTv) {
                dependency(
                    TvAppNavigator(
                        destination = navBackStackEntry.destination,
                        navController = navController,
                        closeApp = closeApp
                    )
                )
            } else {
                dependency(previewFilm)
                dependency(play)
                dependency(
                    MobileAppNavigator(
                        destination = navBackStackEntry.destination,
                        navController = navController,
                        closeApp = closeApp
                    )
                )
            }
        }
    )
}


private val NavDestination.hostNavGraph: NavGraph
    get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
private fun AnimatedContentTransitionScope<*>.defaultEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): EnterTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeIn()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentTransitionScope<*>.defaultExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): ExitTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeOut()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentTransitionScope<*>.defaultPopEnterTransition(): EnterTransition {
    return fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End)
}

@ExperimentalAnimationApi
private fun AnimatedContentTransitionScope<*>.defaultPopExitTransition(): ExitTransition {
    return fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
}