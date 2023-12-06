package com.flixclusive.presentation.mobile.screens.preferences.genre

import androidx.compose.runtime.Composable
import com.flixclusive.presentation.destinations.PreferencesFilmMobileScreenDestination
import com.flixclusive.presentation.mobile.common.composables.genre.GenreScreen
import com.flixclusive.presentation.mobile.common.composables.genre.GenreScreenNavArgs
import com.flixclusive.presentation.mobile.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@PreferencesNavGraph
@Destination(
    navArgsDelegate = GenreScreenNavArgs::class,
    style = CommonScreenTransition::class
)
@Composable
fun PreferencesGenreMobileScreen(
    navigator: DestinationsNavigator,
    mainMobileSharedViewModel: MainMobileSharedViewModel,
    genreArgs: GenreScreenNavArgs
) {
    GenreScreen(
        genre = genreArgs.genre,
        onFilmLongClick = mainMobileSharedViewModel::onFilmLongClick,
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                PreferencesFilmMobileScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
        onNavigationIconClick = navigator::navigateUp
    )
}