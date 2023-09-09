package com.flixclusive.presentation.mobile.screens.search.film

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.destinations.SearchGenreScreenDestination
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.common.composables.film.FilmMobileScreen
import com.flixclusive.presentation.mobile.screens.search.SearchNavGraph
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
    FilmMobileScreen(
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