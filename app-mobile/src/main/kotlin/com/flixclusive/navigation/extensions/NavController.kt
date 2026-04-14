package com.flixclusive.navigation.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.flixclusive.navigation.ROOT
import com.ramcosta.composedestinations.generated.appmobile.AppmobileNavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DirectionNavGraphSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.spec.Route
import com.ramcosta.composedestinations.utils.startDestination

internal fun DestinationsNavigator.bottomBarNavigate(
    screen: DirectionNavGraphSpec,
    currentSelectedScreen: Route,
    currentNavGraph: NavGraphSpec,
) {
    val isPoppingToRoot = screen == currentNavGraph

    if (isPoppingToRoot && currentSelectedScreen == screen.startRoute) {
        return
    }

    navigate(screen) {
        if (isPoppingToRoot) {
            popUpTo(screen.startRoute) {
                inclusive = true
            }
        } else {
            popUpTo(AppmobileNavGraphs.home.startDestination) {
                saveState = true
            }
        }

        launchSingleTop = true
        restoreState = true
    }
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
