package com.flixclusive.navigation.navgraph

import com.flixclusive.navigation.AppDefaultTransition
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.search.destinations.SearchScreenDestination

@NavGraph<AppNavGraph>(defaultTransitions = AppDefaultTransition::class)
internal annotation class SearchNavGraph {
    @ExternalDestination<SearchScreenDestination>(start = true)
    companion object Includes
}
