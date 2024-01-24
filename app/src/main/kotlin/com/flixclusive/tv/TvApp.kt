package com.flixclusive.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.flixclusive.core.ui.tv.util.LocalCurrentRouteProvider
import com.flixclusive.core.ui.tv.util.LocalDrawerWidth
import com.flixclusive.core.ui.tv.util.LocalLastFocusedItemPerDestinationProvider
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.flixclusive.feature.tv.film.destinations.FilmScreenDestination
import com.flixclusive.util.AppNavHost
import com.flixclusive.util.currentScreenAsState
import com.flixclusive.util.navigateSingleTopTo
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import kotlin.system.exitProcess

@Composable
fun TvActivity.TvApp() {
    val navController = rememberNavController()
    val currentSelectedScreen by navController.currentDestinationFlow.collectAsStateWithLifecycle(initialValue = TvNavGraphs.root.startRoute)
    val currentNavGraph by navController.currentScreenAsState(TvNavGraphs.home, isTv = true)

    var isNavDrawerVisible by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentSelectedScreen) {
        isNavDrawerVisible = currentSelectedScreen != FilmScreenDestination
            && currentSelectedScreen != SplashScreenDestination
    }

    val currentScreenIndexSelected = remember(currentNavGraph) {
        tvNavigationItems.indexOfFirst { it.screen == currentNavGraph }
    }

    LaunchedEffect(isDrawerOpen) {
        if(isDrawerOpen) {
            NavItemsFocusRequesters.getOrNull(currentScreenIndexSelected)?.requestFocus()
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

    CompositionLocalProvider(LocalDrawerWidth provides InitialDrawerWidth) {
        LocalLastFocusedItemPerDestinationProvider {
            LocalCurrentRouteProvider(currentSelectedScreen = currentSelectedScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    NavDrawer(
                        currentScreen = currentNavGraph,
                        onNavigate = { screen ->
                            navController.run {
                                val isPoppingToRoot = screen == currentNavGraph

                                navigateSingleTopTo(
                                    direction = screen,
                                    isPoppingToRoot = isPoppingToRoot
                                )
                            }
                        },
                        isNavDrawerVisible = isNavDrawerVisible,
                        isDrawerOpen = isDrawerOpen,
                        onDrawerStateChange = { isDrawerOpen = it },
                    ) {
                        AppNavHost(
                            navController = navController,
                            isTv = true,
                            closeApp = {
                                finish()
                                exitProcess(0)
                            }
                        )
                    }
                }
            }
        }
    }
}