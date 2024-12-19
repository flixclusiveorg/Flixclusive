package com.flixclusive.feature.mobile.user.edit

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination

data class UserEditScreenNavArgs(
    val user: User
)

@Destination(
    navArgsDelegate = UserEditScreenNavArgs::class
)
@Composable
internal fun UserEditScreen(
    navigator: GoBackAction,
    userArg: User
) {
    var user by remember { mutableStateOf(userArg) }
    // TODO: Add edit screen
}

@Preview
@Composable
private fun UserEditScreenBasePreview() {
    FlixclusiveTheme {
        Surface {

        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun UserEditScreenCompactLandscapePreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun UserEditScreenMediumPortraitPreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun UserEditScreenMediumLandscapePreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun UserEditScreenExtendedPortraitPreview() {
    UserEditScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun UserEditScreenExtendedLandscapePreview() {
    UserEditScreenBasePreview()
}