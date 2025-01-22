package com.flixclusive.feature.mobile.library

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewFilmAction
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination

interface LibraryScreenNavigator : GoBackAction, ViewFilmAction

@Destination
@Composable
internal fun LibraryScreen(
    navigator: LibraryScreenNavigator,
    viewModel: LibraryScreenViewModel = hiltViewModel(),
    previewFilm: (Film) -> Unit,
) {

}

@Composable
private fun LibraryScreen() {

}

@Preview
@Composable
private fun LibraryScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            LibraryScreen()
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LibraryScreenCompactLandscapePreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun LibraryScreenMediumPortraitPreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun LibraryScreenMediumLandscapePreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun LibraryScreenExtendedPortraitPreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun LibraryScreenExtendedLandscapePreview() {
    LibraryScreenBasePreview()
}
