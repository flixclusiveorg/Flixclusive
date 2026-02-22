package com.flixclusive.navigation.navgraph

import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.home.destinations.HomeScreenDestination

@NavGraph<AppNavGraph>
internal annotation class HomeNavGraph {
    @ExternalDestination<HomeScreenDestination>(start = true)
    companion object Includes
}
