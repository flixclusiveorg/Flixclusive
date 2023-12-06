package com.flixclusive.presentation.mobile.screens.search.see_all

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.common.viewmodels.see_all.SeeAllScreenNavArgs
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.mobile.common.composables.see_all.SeeAllMobileScreen
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.screens.search.SearchNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@SearchNavGraph
@Destination(
    navArgsDelegate = SeeAllScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun SearchSeeAllScreen(
    navigator: DestinationsNavigator,
    mainMobileSharedViewModel: MainMobileSharedViewModel
) {
    SeeAllMobileScreen(
        onFilmLongClick = mainMobileSharedViewModel::onFilmLongClick,
        onNavigationIconClick = navigator::navigateUp,
        onFilmClick = {
            navigator.navigate(
                SearchFilmScreenDestination(
                    film = it
                ),
                onlyIfResumed = true
            )
        },
    )
}