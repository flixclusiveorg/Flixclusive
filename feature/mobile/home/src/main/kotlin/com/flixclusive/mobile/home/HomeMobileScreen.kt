package com.flixclusive.mobile.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.mobile.MainMobileSharedViewModel
import com.flixclusive.core.ui.mobile.OnDoubleNavBarItemClickObserver
import com.flixclusive.core.ui.mobile.OnSeeMoreDetailsClickObserver
import com.flixclusive.core.util.navigation.navigateSingleTopTo
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.destinations.HomeScreenContentDestination
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

@MobileRootNavGraph(start = true)
@NavGraph
annotation class HomeNavGraph(
    val start: Boolean = false
)

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@com.ramcosta.composedestinations.annotation.Destination(
    style = HomeScreenTransition::class
)
@Composable
fun HomeMobileScreen(
    mainMobileSharedViewModel: MainMobileSharedViewModel,
) {
    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()

    val startDestination = HomeScreenContentDestination
    val navGraph = NavGraphs.home
    val currentScreen: Destination = navController.appCurrentDestinationAsState().value
        ?: startDestination

    val navGraphThatNeedsToGoToRoot by rememberUpdatedState(newValue = mainMobileSharedViewModel.navGraphThatNeedsToGoToRoot)
    val mainUiState by mainMobileSharedViewModel.uiState.collectAsStateWithLifecycle()
    val filmToPreview by mainMobileSharedViewModel.filmToPreview.collectAsStateWithLifecycle()

    OnSeeMoreDetailsClickObserver(
        isSeeingMoreDetailsProvider = { mainUiState.isSeeingMoreDetailsOfLongClickedFilm },
        currentBackStackEntryProvider = { navController.currentBackStackEntry },
        navigate = {
            // consume it
            navController.navigate(
                HomeFilmScreenDestination(filmToPreview!!)
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