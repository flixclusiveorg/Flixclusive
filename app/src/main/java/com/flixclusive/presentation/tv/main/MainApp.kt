package com.flixclusive.presentation.tv.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.common.viewmodels.configuration.AppConfigurationViewModel
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.FilmTvScreenDestination
import com.flixclusive.presentation.destinations.HomeTvScreenDestination
import com.flixclusive.presentation.startAppDestination
import com.flixclusive.presentation.tv.screens.splash_screen.SplashTvScreen
import com.flixclusive.presentation.utils.ComposeUtils.navigateSingleTopTo
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TVMainActivity.MainApp() {
    val appConfigurationViewModel: AppConfigurationViewModel = hiltViewModel()
    val appViewModel: MainTvSharedViewModel = hiltViewModel()

    val splashScreenUiState by appConfigurationViewModel.state.collectAsStateWithLifecycle()
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()

    val startDestination = NavGraphs.tvRoot.startAppDestination

    var currentScreen by remember { mutableStateOf(startDestination) }
    val currentScreenState: Destination = navController.appCurrentDestinationAsState().value ?: startDestination

    var isNavDrawerVisible by remember { mutableStateOf(true) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentScreenState) {
        if(currentScreenState == FilmTvScreenDestination) {
            isNavDrawerVisible = false
            return@LaunchedEffect
        }

        isNavDrawerVisible = true
        currentScreen = currentScreenState
    }

    val currentScreenIndexSelected = remember(currentScreen) {
        TvAppDestination.values().indexOfFirst {
            it.direction == currentScreen
            || it.direction.route.contains(
                other = currentScreen.baseRoute,
                ignoreCase = true
            )
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if(splashScreenUiState.isShowingTvScreen) {
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
                        dependency(appViewModel)
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = !appState.isHidingSplashScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SplashTvScreen(
                uiState = splashScreenUiState,
                dismissDialog = appConfigurationViewModel::onConsumeUpdateDialog,
                onExitApp = ::finishAndRemoveTask,
                onStartApp = appConfigurationViewModel::showTvScreen
            )
        }
    }
}