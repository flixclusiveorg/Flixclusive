package com.flixclusive.feature.mobile.user.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface UserEditScreenNavigator : GoBackAction {
    fun openUserAvatarSelectScreen()
}

data class UserEditScreenNavArgs(
    val user: User
)

@Destination(
    navArgsDelegate = UserEditScreenNavArgs::class
)
@Composable
internal fun UserEditScreen(
    navigator: UserEditScreenNavigator,
    resultRecipient: ResultRecipient<UserAvatarSelectScreenDestination, Int>,
    userArg: UserEditScreenNavArgs
) {
    var user by remember { mutableStateOf(userArg.user) }
    var name by remember { mutableStateOf(userArg.user.name.createTextFieldValue()) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(LocaleR.string.edit_profile),
                onNavigate = { navigator.goBack() }
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(getAdaptiveDp(5.dp))
                ) {
                    UserAvatar(
                        user = user,
                        borderWidth = 0.dp,
                        shadowBlur = 0.dp,
                        shadowSpread = 0.dp,
                        modifier = Modifier
                            .height(
                                getAdaptiveDp(
                                    dp = (DefaultAvatarSize.value * 1.5).dp,
                                    incrementedDp = 100.dp
                                )
                            )
                            .aspectRatio(1F)
                    )
                }

                ChangeImageButton(
                    onClick = {
                        navigator.openUserAvatarSelectScreen()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                )
            }

            TextField(
                value = name,
                onValueChange = { name = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                ),
                shape = MaterialTheme.shapes.extraSmall,
                readOnly = true,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .height(
                        getAdaptiveDp(
                            dp = 65.dp,
                            incrementedDp = 15.dp
                        )
                    )
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChangeImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSize = getAdaptiveDp(30.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .clickable(
                indication = ripple(
                    bounded = false,
                    radius = buttonSize / 2
                ),
                interactionSource = null,
            ) {
                onClick()
            }
    ) {
        Icon(
            painter = painterResource(UiCommonR.drawable.edit),
            contentDescription = stringResource(LocaleR.string.change_avatar_content_desc),
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(
                getAdaptiveDp(
                    dp = 18.dp,
                    incrementedDp = 10.dp
                ),
            )
        )
    }
}

@Preview
@Composable
private fun UserEditScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            UserEditScreen(
                navigator = object: UserEditScreenNavigator {
                    override fun openUserAvatarSelectScreen() = Unit
                    override fun goBack() = Unit
                },
                resultRecipient = object : ResultRecipient<UserAvatarSelectScreenDestination, Int> {
                    @Composable
                    override fun onNavResult(listener: (NavResult<Int>) -> Unit) = Unit
                    @Composable
                    override fun onResult(listener: (Int) -> Unit) = Unit
                },
                userArg = UserEditScreenNavArgs(
                    user = User(
                        id = 0,
                        image = 0,
                        name = "John Doe"
                    )
                )
            )
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