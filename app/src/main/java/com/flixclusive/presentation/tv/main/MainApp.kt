package com.flixclusive.presentation.tv.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.FilmTvScreenDestination
import com.flixclusive.presentation.destinations.HomeTvScreenDestination
import com.flixclusive.presentation.startAppDestination
import com.flixclusive.presentation.utils.ComposeUtils.navigateSingleTopTo
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TVMainActivity.MainApp() {
    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()
    val currentScreen: Destination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.tvRoot.startAppDestination

    var isNavDrawerVisible by remember { mutableStateOf(true) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    val currentScreenIndexSelected = remember(currentScreen) {
        TvAppDestination.values().indexOfFirst {
            it.direction == currentScreen
            || it.direction.route.contains(
                other = currentScreen.baseRoute,
                ignoreCase = true
            )
        }
    }

    LaunchedEffect(currentScreen) {
        isNavDrawerVisible = !FilmTvScreenDestination.route.contains(currentScreen.baseRoute)
    }

    LaunchedEffect(isDrawerOpen) {
        if(isDrawerOpen) {
            NavItemsFocusRequesters[currentScreenIndexSelected].requestFocus()
        }
    }

    BackHandler(
        enabled = isNavDrawerVisible,
        onBack = {
            when {
                // 1. On user's first back press, bring focus to the current selected tab, if TopBar is not
                //    visible, first make it visible, then focus the selected tab
                // 2. On second back press, bring focus back to the first displayed tab
                // 3. On third back press, exit the app

                !isDrawerOpen -> isDrawerOpen = true
                currentScreenIndexSelected == 0 -> finish()
                else -> NavItemsFocusRequesters[0].requestFocus()
            }
        }
    )

    NavDrawer(
        currentScreen = currentScreen,
        onNavigate = {
            navController.navigateSingleTopTo(it, NavGraphs.tvRoot)
        },
        isNavDrawerVisible = isNavDrawerVisible,
        isDrawerOpen = isDrawerOpen,
        onDrawerStateChange = { isDrawerOpen = it },
    ) {
        DestinationsNavHost(
            engine = engine,
            navController = navController,
            navGraph = NavGraphs.tvRoot,
            startRoute = HomeTvScreenDestination,
            dependenciesContainerBuilder = {
                //dependency(viewModel)
            }
        )
    }
}