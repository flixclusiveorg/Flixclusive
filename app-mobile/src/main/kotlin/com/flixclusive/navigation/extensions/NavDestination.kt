package com.flixclusive.navigation.extensions

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.flixclusive.navigation.MobileNavGraphs
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal fun NavDestination.navGraph(): NavGraphSpec {
    hierarchy.forEach { destination ->
        MobileNavGraphs.root.nestedNavGraphs.forEach { navGraph ->
            if (destination.route == navGraph.route) {
                return navGraph
            }
        }
    }

    throw RuntimeException("Unknown nav graph for destination $route")
}
