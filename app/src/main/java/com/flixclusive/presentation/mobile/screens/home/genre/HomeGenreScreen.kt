package com.flixclusive.presentation.mobile.screens.home.genre

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.screens.home.HomeNavGraph
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.common.composables.genre.GenreScreen
import com.flixclusive.presentation.mobile.common.composables.genre.GenreScreenNavArgs
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@HomeNavGraph
@Destination(
    navArgsDelegate = GenreScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun HomeGenreScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    genreArgs: GenreScreenNavArgs,
) {
    GenreScreen(
        genre = genreArgs.genre,
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                HomeFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onNavigationIconClick = navigator::navigateUp
    )
}