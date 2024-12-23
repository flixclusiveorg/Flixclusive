package com.flixclusive.feature.mobile.user.add.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.user.add.OnBoardingScreen
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.LocalUserToAdd
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object NameScreen : OnBoardingScreen {
    override val index: Int = 0
    override val title: UiText = UiText.StringResource(LocaleR.string.onboarding_profile_name_title)
    override val description: UiText = UiText.StringResource(LocaleR.string.onboarding_profile_name_description)

    @Composable
    override fun Content(modifier: Modifier) {
        val userToAdd = LocalUserToAdd.current

        var isError by remember { mutableStateOf(false) }
        var name by remember {
            mutableStateOf(userToAdd.value.name.createTextFieldValue())
        }
        val defaultContainerColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(1.dp)

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(name) {
            isError = name.text.isEmpty()
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                    keyboardController?.hide()
                }
            ),
            isError = isError,
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.name),
                    style = getAdaptiveTextStyle(
                        style = TypographyStyle.Label,
                        mode = TextStyleMode.Emphasized,
                    ).copy(
                        color = LocalContentColor.current.onMediumEmphasis(),
                    )
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = name.text.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = { name = "".createTextFieldValue() }
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(LocaleR.string.clear_text_button),
                            modifier = Modifier
                                .size(getAdaptiveDp(20.dp, 8.dp))
                        )
                    }
                }
            },
            textStyle = getAdaptiveTextStyle(
                size = 16.sp,
                increaseBy = 10.sp,
                style = TypographyStyle.Body,
                mode = TextStyleMode.Normal,
            ).copy(
                textAlign = TextAlign.Start
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = defaultContainerColor,
                focusedContainerColor = defaultContainerColor,
                errorContainerColor = defaultContainerColor,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            modifier = modifier
                .height(
                    getAdaptiveDp(
                        dp = 65.dp,
                        increaseBy = 18.dp
                    )
                )
        )
    }

    @Composable
    override fun OnBoardingIcon() {
        val orientation = LocalConfiguration.current.orientation
        val sizeDp = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getAdaptiveDp(150.dp, 100.dp)
        } else getAdaptiveDp(250.dp, 300.dp, 500.dp)

        Icon(
            painter = painterResource(UiCommonR.drawable.happy_emphasized),
            contentDescription = stringResource(LocaleR.string.icon_for_name_screen_content_desc),
            tint = MaterialTheme.colorScheme.primary.onMediumEmphasis(0.8F),
            modifier = Modifier
                .size(sizeDp)
        )
    }

}

@Preview
@Composable
private fun NameScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NameScreen.Content()
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun NameScreenCompactLandscapePreview() {
    NameScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun NameScreenMediumPortraitPreview() {
    NameScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun NameScreenMediumLandscapePreview() {
    NameScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun NameScreenExtendedPortraitPreview() {
    NameScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun NameScreenExtendedLandscapePreview() {
    NameScreenBasePreview()
}