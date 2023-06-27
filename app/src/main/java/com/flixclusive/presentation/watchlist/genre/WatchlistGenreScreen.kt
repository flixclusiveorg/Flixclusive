package com.flixclusive.presentation.watchlist.genre

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.WatchlistFilmScreenDestination
import com.flixclusive.presentation.genre.GenreScreen
import com.flixclusive.presentation.genre.GenreScreenNavArgs
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.watchlist.WatchlistNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@WatchlistNavGraph
@Destination(
    navArgsDelegate = GenreScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun WatchlistGenreScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    genreArgs: GenreScreenNavArgs,
) {
    GenreScreen(
        genre = genreArgs.genre,
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                WatchlistFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onNavigationIconClick = navigator::navigateUp
    )
}