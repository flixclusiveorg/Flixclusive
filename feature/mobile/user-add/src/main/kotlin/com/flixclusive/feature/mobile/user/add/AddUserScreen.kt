package com.flixclusive.feature.mobile.user.add

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.CommonUserEditNavigator
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveModifierUtil.fillMaxAdaptiveWidth
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.feature.mobile.user.add.component.AddUserScaffold
import com.flixclusive.feature.mobile.user.add.screens.NameScreen
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.ProvideUserToAdd
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.OpenResultRecipient
import kotlinx.collections.immutable.persistentListOf


@Destination
@Composable
internal fun AddUserScreen(
    navigator: CommonUserEditNavigator,
    resultRecipient: OpenResultRecipient<Int>
) {
    var userAvatar by rememberSaveable { mutableIntStateOf(-1) }
//    resultRecipient.onNavResult { result ->
//        if (result is NavResult.Value) {
//            userAvatar = result.value
//        }
//    }

    var currentScreen by remember { mutableIntStateOf(0) }
    val screens = remember {
        persistentListOf(
            NameScreen,
        )
    }

    ProvideUserToAdd {
        AddUserScaffold(
            onBack = navigator::goBack
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = getAdaptiveDp(16.dp)
                    )
            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .noIndicationClickable {
//                            focusManager.clearFocus(true)
//                            keyboardController?.hide()
//                        }
//                )


                AnimatedContent(
                    targetState = screens[currentScreen],
                    label = "OnBoardingProfileScreens",
                    modifier = Modifier.fillMaxSize(),
                ) { screen ->
                    if (screen is NameScreen) {
                        val focusManager = LocalFocusManager.current
                        val keyboardController = LocalSoftwareKeyboardController.current

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .noIndicationClickable {
                                    focusManager.clearFocus(true)
                                    keyboardController?.hide()
                                }
                        )
                    }

                    with(screen) {
                        val orientation = LocalConfiguration.current.orientation
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            AddUserPortraitScreen()
                        } else {
                            AddUserLandscapeScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnBoardingScreen.AddUserPortraitScreen() {
    val widthModifier = Modifier.fillMaxAdaptiveWidth(
        medium = 0.8F,
        expanded = 0.9F
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = getAdaptiveDp(dp = 5.dp, increaseBy = 3.dp),
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxSize()
    ) {
        OnBoardingIcon()

        Text(
            text = title.asString(),
            style = getAdaptiveTextStyle(
                size = 22.sp,
                increaseBy = 16.sp,
                style = TypographyStyle.Headline,
                mode = TextStyleMode.Emphasized
            ),
            modifier = Modifier
                .padding(top = 20.dp)
        )

        Text(
            text = description.asString(),
            style = getAdaptiveTextStyle(
                size = 14.sp,
                increaseBy = 10.sp,
                style = TypographyStyle.Body,
                mode = TextStyleMode.NonEmphasized
            ),
            modifier = widthModifier
                .padding(bottom = getAdaptiveDp(10.dp))
        )

        Content(widthModifier)
    }
}

@Composable
private fun OnBoardingScreen.AddUserLandscapeScreen() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
    ) {
        OnBoardingIcon()

        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title.asString(),
                style = getAdaptiveTextStyle(
                    size = 30.sp,
                    increaseBy = 10.sp,
                    style = TypographyStyle.Display,
                    mode = TextStyleMode.Emphasized
                ),
                modifier = Modifier
                    .padding(top = 20.dp)
            )

            Text(
                text = description.asString(),
                style = getAdaptiveTextStyle(
                    size = 20.sp,
                    style = TypographyStyle.Body,
                    mode = TextStyleMode.NonEmphasized
                ),
                modifier = Modifier
                    .padding(bottom = 10.dp)
            )

            Content(
                Modifier.fillMaxAdaptiveWidth(
                    medium = 0.6F,
                    expanded = 0.6F
                )
            )
        }
    }
}

@SuppressLint("ComposableNaming")
@Preview
@Composable
private fun AddUserScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            AddUserScreen(
                navigator = object: CommonUserEditNavigator {
                    override fun openUserAvatarSelectScreen() = Unit
                    override fun goBack() = Unit
                },
                resultRecipient = object: OpenResultRecipient<Int> {
                    @Composable
                    override fun onNavResult(listener: (NavResult<Int>) -> Unit) = Unit
                }
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun AddUserScreenCompactLandscapePreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun AddUserScreenMediumPortraitPreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun AddUserScreenMediumLandscapePreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun AddUserScreenExtendedPortraitPreview() {
    AddUserScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun AddUserScreenExtendedLandscapePreview() {
    AddUserScreenBasePreview()
}