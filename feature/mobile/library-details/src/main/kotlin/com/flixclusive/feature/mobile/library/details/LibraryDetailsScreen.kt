package com.flixclusive.feature.mobile.library.details

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.model.database.LibraryList
import com.ramcosta.composedestinations.annotation.Destination

interface LibraryDetailsScreenNavigator : GoBackAction {
    // TODO: Add navigator to AddLibraryItemScreen
}

data class LibraryDetailsNavArgs(
    val library: LibraryList
)

@Destination(
    navArgsDelegate = LibraryDetailsNavArgs::class
)
@Composable
internal fun LibraryDetailsScreen(
    navigator: LibraryDetailsScreenNavigator
) {

}

@Composable
internal fun LibraryDetailsScreen() {

}


@Preview
@Composable
private fun LibraryDetailsScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            LibraryDetailsScreen()
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LibraryDetailsScreenCompactLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun LibraryDetailsScreenMediumPortraitPreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun LibraryDetailsScreenMediumLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun LibraryDetailsScreenExtendedPortraitPreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun LibraryDetailsScreenExtendedLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}
