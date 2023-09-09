package com.flixclusive.presentation.mobile.screens.preferences.film

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.destinations.PreferencesFilmMobileScreenDestination
import com.flixclusive.presentation.destinations.PreferencesGenreMobileScreenDestination
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.common.composables.film.FilmMobileScreen
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@UnstableApi
@PreferencesNavGraph
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun PreferencesFilmMobileScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
) {
    FilmMobileScreen(
        onNavigationIconClick = navigator::navigateUp,
        onGenreClick = { genre ->
            navigator.navigate(
                PreferencesGenreMobileScreenDestination(genre = genre),
                onlyIfResumed = true
            )
        },
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                PreferencesFilmMobileScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onPlayClick = mainSharedViewModel::onPlayClick
    )
}