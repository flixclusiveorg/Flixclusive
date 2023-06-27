package com.flixclusive.presentation.recently_watched.genre

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.RecentlyWatchedFilmScreenDestination
import com.flixclusive.presentation.genre.GenreScreen
import com.flixclusive.presentation.genre.GenreScreenNavArgs
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.recently_watched.RecentNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@RecentNavGraph
@Destination(
    navArgsDelegate = GenreScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun RecentlyWatchedGenreScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    genreArgs: GenreScreenNavArgs
) {
    GenreScreen(
        genre = genreArgs.genre,
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                RecentlyWatchedFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onNavigationIconClick = navigator::navigateUp
    )
}