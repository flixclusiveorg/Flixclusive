package com.flixclusive.navigation.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.flixclusive.navigation.MobileNavGraphs
import com.flixclusive.navigation.ROOT
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.utils.startDestination

internal fun NavController.bottomBarNavigate(
    screen: NavGraphSpec,
    currentNavGraph: NavGraphSpec,
) {
    val isPoppingToRoot = screen == currentNavGraph

    navigate(screen) {
        if (isPoppingToRoot) {
            popUpTo(screen.startRoute.route)
        } else {
            popUpTo(MobileNavGraphs.home.startDestination.route) {
                saveState = true
            }
        }

        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavController.navigateIfResumed(
    direction: Direction,
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {},
) {
    val isResumed = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED
    if (isResumed == false) {
        return
    }

    navigate(direction, navOptionsBuilder)
}

@Stable
@Composable
internal fun NavController.currentScreenAsState(initialNavGraph: NavGraphSpec): State<NavGraphSpec> {
    val selectedItem = remember { mutableStateOf(initialNavGraph) }

    DisposableEffect(this) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val doesNotHaveNavGraph = destination.parent?.route == ROOT
            if (doesNotHaveNavGraph) {
                return@OnDestinationChangedListener
            }

            selectedItem.value = destination.navGraph()
        }

        addOnDestinationChangedListener(listener)

        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }

    return selectedItem
}
