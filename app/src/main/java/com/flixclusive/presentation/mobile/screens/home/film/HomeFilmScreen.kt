package com.flixclusive.presentation.mobile.screens.home.film

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.destinations.HomeGenreScreenDestination
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.common.composables.film.FilmMobileScreen
import com.flixclusive.presentation.mobile.screens.home.HomeNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@UnstableApi
@HomeNavGraph
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun HomeFilmScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
) {
    FilmMobileScreen(
        onNavigationIconClick = navigator::navigateUp,
        onGenreClick = { genre ->
            navigator.navigate(
                HomeGenreScreenDestination(genre = genre),
                onlyIfResumed = true
            )
        },
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                HomeFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onPlayClick = mainSharedViewModel::onPlayClick
    )
}