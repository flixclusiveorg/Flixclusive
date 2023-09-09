package com.flixclusive.presentation.mobile.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenContentDestination
import com.flixclusive.presentation.destinations.SearchScreenExpandedDestination
import com.flixclusive.presentation.mobile.common.MobileRootNavGraph
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.main.OnDoubleNavBarItemClickObserver
import com.flixclusive.presentation.mobile.main.OnSeeMoreDetailsClickObserver
import com.flixclusive.presentation.utils.ComposeUtils.navigateSingleTopTo
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate

@MobileRootNavGraph
@NavGraph
annotation class SearchNavGraph(
    val start: Boolean = false
)

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@com.ramcosta.composedestinations.annotation.Destination(
    style = SearchMobileScreenTransition::class
)
@Composable
fun SearchMobileScreen(
    mainSharedViewModel: MainSharedViewModel,
) {
    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()

    val startDestination = SearchScreenContentDestination
    val navGraph = NavGraphs.search
    val currentScreen: Destination = navController.appCurrentDestinationAsState().value
        ?: startDestination

    val navGraphThatNeedsToGoToRoot by rememberUpdatedState(newValue = mainSharedViewModel.navGraphThatNeedsToGoToRoot)
    val mainUiState by mainSharedViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreen) {
        mainSharedViewModel.onBottomNavigationBarVisibilityChange(
            newVisibilityValue = currentScreen != SearchScreenExpandedDestination
        )
    }

    OnSeeMoreDetailsClickObserver(
        isSeeingMoreDetailsProvider = { mainUiState.isSeeingMoreDetailsOfLongClickedFilm },
        currentBackStackEntryProvider = { navController.currentBackStackEntry },
        navigate = {
            // consume it
            navController.navigate(
                SearchFilmScreenDestination(mainUiState.longClickedFilm!!)
            )
            mainSharedViewModel.onBottomSheetClose()
            mainSharedViewModel.onSeeMoreClick(shouldSeeMore = false)
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
        consume = mainSharedViewModel::onNavBarItemClickTwice
    )

    DestinationsNavHost(
        engine = engine,
        navController = navController,
        navGraph = navGraph,
        startRoute = startDestination,
        dependenciesContainerBuilder = {
            dependency(mainSharedViewModel)
        }
    )
}
