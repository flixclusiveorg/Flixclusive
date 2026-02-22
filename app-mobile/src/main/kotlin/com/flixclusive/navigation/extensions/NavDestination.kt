package com.flixclusive.navigation.extensions

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal fun NavDestination.navGraph(): NavGraphSpec {
    hierarchy.forEach { destination ->
        if (destination.route == route) {
            return@forEach
        }

        if (destination.route == AppGraph.route) {
            return AppGraph
        }

        AppGraph.nestedNavGraphs.forEach { navGraph ->
            if (destination.route == navGraph.route) {
                return navGraph
            }
        }
    }

    throw RuntimeException("Unknown nav graph for destination $route")
}
