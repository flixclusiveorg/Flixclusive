package com.flixclusive.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry

@Composable
fun OnSeeMoreDetailsClickObserver(
    isSeeingMoreDetailsProvider: () -> Boolean,
    currentBackStackEntryProvider: () -> NavBackStackEntry?,
    navigate: () -> Unit
) {
    val isSeeingMoreDetails = remember(isSeeingMoreDetailsProvider()) { isSeeingMoreDetailsProvider() }
    val currentBackStackEntry = remember(currentBackStackEntryProvider()) { currentBackStackEntryProvider() }

    LaunchedEffect(key1 = isSeeingMoreDetails) {
        if(isSeeingMoreDetails && currentBackStackEntry?.lifecycleIsResumed() == true) {
            navigate()
        }
    }
}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
private fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED