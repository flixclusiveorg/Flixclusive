package com.flixclusive.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.presentation.NavGraph
import com.flixclusive.presentation.destinations.Destination
import com.ramcosta.composedestinations.spec.NavGraphSpec

@Composable
fun OnDoubleNavBarItemClickObserver(
    navGraphThatNeedsToGoToRootProvider: () -> NavGraphSpec?,
    navGraph: NavGraph,
    currentScreen: Destination,
    startDestination: Destination,
    navigate: () -> Unit,
    consume: (NavGraphSpec?) -> Unit
) {
    val navGraphThatNeedsToGoToRoot by rememberUpdatedState(newValue = navGraphThatNeedsToGoToRootProvider())

    LaunchedEffect(key1 = navGraphThatNeedsToGoToRoot) {
        if(navGraphThatNeedsToGoToRoot == navGraph && currentScreen != startDestination) {
            navigate()
        }

        consume(null) // consume it
    }
}