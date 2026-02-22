package com.flixclusive.navigation.navgraph

import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.search.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.searchexpanded.destinations.SearchExpandedScreenDestination

@NavGraph<AppNavGraph>
internal annotation class SearchNavGraph {
    @ExternalDestination<SearchScreenDestination>(start = true)
    @ExternalDestination<SearchExpandedScreenDestination>
    companion object Includes
}
