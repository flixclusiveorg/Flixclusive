package com.flixclusive.navigation.navgraph

import com.flixclusive.navigation.AppDefaultTransition
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.home.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.search.destinations.SearchScreenDestination

@NavGraph<AppNavGraph>(defaultTransitions = AppDefaultTransition::class)
internal annotation class HomeNavGraph {
    @ExternalDestination<HomeScreenDestination>(start = true)
    @ExternalDestination<SearchScreenDestination>
    companion object Includes
}
