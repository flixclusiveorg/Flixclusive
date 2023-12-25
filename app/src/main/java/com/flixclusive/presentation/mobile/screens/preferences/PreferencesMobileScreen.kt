package com.flixclusive.presentation.mobile.screens.preferences

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.PreferencesFilmMobileScreenDestination
import com.flixclusive.presentation.destinations.PreferencesRootMobileScreenDestination
import com.flixclusive.presentation.mobile.common.MobileRootNavGraph
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.main.utils.OnDoubleNavBarItemClickObserver
import com.flixclusive.presentation.mobile.main.utils.OnSeeMoreDetailsClickObserver
import com.flixclusive.presentation.utils.ComposeUtils.navigateSingleTopTo
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate

@MobileRootNavGraph
@NavGraph
annotation class PreferencesNavGraph(
    val start: Boolean = false
)
@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@com.ramcosta.composedestinations.annotation.Destination(
    style = PreferencesMobileScreenTransition::class
)
@Composable
fun PreferencesMobileScreen(
    mainMobileSharedViewModel: MainMobileSharedViewModel,
) {
    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()

    val startDestination = PreferencesRootMobileScreenDestination
    val navGraph = NavGraphs.preferences
    val currentScreen: Destination = navController.appCurrentDestinationAsState().value
        ?: startDestination

    val navGraphThatNeedsToGoToRoot by rememberUpdatedState(newValue = mainMobileSharedViewModel.navGraphThatNeedsToGoToRoot)
    val mainUiState by mainMobileSharedViewModel.uiState.collectAsStateWithLifecycle()

    OnSeeMoreDetailsClickObserver(
        isSeeingMoreDetailsProvider = { mainUiState.isSeeingMoreDetailsOfLongClickedFilm },
        currentBackStackEntryProvider = { navController.currentBackStackEntry },
        navigate = {
            // consume it
            navController.navigate(
                PreferencesFilmMobileScreenDestination(mainUiState.longClickedFilm!!)
            )
            mainMobileSharedViewModel.onBottomSheetClose()
            mainMobileSharedViewModel.onSeeMoreClick(shouldSeeMore = false)
        }
    )

    OnDoubleNavBarItemClickObserver(
        navGraphThatNeedsToGoToRootProvider = { navGraphThatNeedsToGoToRoot },
        currentScreen = currentScreen,
        startDestination = startDestination,
        navGraph = navGraph,
        navigate = {
            navController.navigateSingleTopTo(startDestination, NavGraphs.mobileRoot)
        },
        consume = mainMobileSharedViewModel::onNavBarItemClickTwice
    )

    DestinationsNavHost(
        engine = engine,
        navController = navController,
        navGraph = navGraph,
        startRoute = startDestination,
        dependenciesContainerBuilder = {
            dependency(mainMobileSharedViewModel)
        }
    )
}