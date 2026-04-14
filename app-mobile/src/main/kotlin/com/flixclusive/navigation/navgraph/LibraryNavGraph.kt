package com.flixclusive.navigation.navgraph

import com.flixclusive.navigation.AppDefaultTransition
import com.ramcosta.composedestinations.annotation.ExternalDestination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.generated.librarydetails.destinations.LibraryDetailsScreenDestination
import com.ramcosta.composedestinations.generated.librarymanage.destinations.ManageLibraryScreenDestination

@NavGraph<AppNavGraph>(defaultTransitions = AppDefaultTransition::class)
internal annotation class LibraryNavGraph {
    @ExternalDestination<ManageLibraryScreenDestination>(start = true)
    @ExternalDestination<LibraryDetailsScreenDestination>
    companion object Includes
}
