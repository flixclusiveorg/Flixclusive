package com.flixclusive.core.util.navigation

import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.Route

fun NavHostController.navigateSingleTopTo(
    direction: Direction,
    route: Route
) = this.navigate(direction) {
    // Pop up to the start destination of the graph to
    // avoid building up a large stack of destinations
    // on the back stack as users select items
    popUpTo(route) {
        saveState = true
    }

    // Avoid multiple copies of the same destination when
    // re-selecting the same item
    launchSingleTop = true
    // Restore uiState when re-selecting a previously selected item
    restoreState = true
}