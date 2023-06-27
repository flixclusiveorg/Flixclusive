package com.flixclusive.presentation.search.film

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.destinations.SearchGenreScreenDestination
import com.flixclusive.presentation.film.FilmScreen
import com.flixclusive.presentation.film.FilmScreenNavArgs
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.search.SearchNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@UnstableApi
@SearchNavGraph
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun SearchFilmScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
) {
    FilmScreen(
        onNavigationIconClick = navigator::navigateUp,
        onGenreClick = { genre ->
            navigator.navigate(
                SearchGenreScreenDestination(genre = genre),
                onlyIfResumed = true
            )
        },
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                SearchFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onPlayClick = mainSharedViewModel::onPlayClick
    )
}