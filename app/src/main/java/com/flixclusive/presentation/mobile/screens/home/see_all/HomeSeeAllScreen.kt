package com.flixclusive.presentation.mobile.screens.home.see_all

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.common.viewmodels.see_all.SeeAllScreenNavArgs
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.mobile.common.composables.see_all.SeeAllMobileScreen
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.screens.home.HomeNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@HomeNavGraph
@Destination(
    navArgsDelegate = SeeAllScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun HomeSeeAllScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel
) {
    SeeAllMobileScreen(
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onNavigationIconClick = navigator::navigateUp,
        onFilmClick = {
            navigator.navigate(
                HomeFilmScreenDestination(
                    film = it
                ),
                onlyIfResumed = true
            )
        },
    )
}