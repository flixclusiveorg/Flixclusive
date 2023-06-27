package com.flixclusive.presentation.home.film

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.destinations.HomeGenreScreenDestination
import com.flixclusive.presentation.film.FilmScreen
import com.flixclusive.presentation.film.FilmScreenNavArgs
import com.flixclusive.presentation.home.HomeNavGraph
import com.flixclusive.presentation.main.MainSharedViewModel
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
    FilmScreen(
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