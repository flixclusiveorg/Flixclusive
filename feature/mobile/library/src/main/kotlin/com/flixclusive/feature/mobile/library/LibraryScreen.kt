package com.flixclusive.feature.mobile.library

import androidx.compose.runtime.Composable
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewFilmAction
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination

interface LibraryScreenNavigator : GoBackAction, ViewFilmAction

@Destination
@Composable
internal fun LibraryScreen(
    navigator: LibraryScreenNavigator,
    // viewModel: RecentlyWatchedScreenViewModel = hiltViewModel(),
    previewFilm: (Film) -> Unit,
) {

}
