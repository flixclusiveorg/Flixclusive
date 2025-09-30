package com.flixclusive.feature.mobile.user.add.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.user.add.OnBoardingScreen
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.LocalUserToAdd
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal object NameScreen : OnBoardingScreen {
    override val index: Int = 0
    override val title: UiText = UiText.StringResource(LocaleR.string.onboarding_profile_name_title)
    override val description: UiText = UiText.StringResource(LocaleR.string.onboarding_profile_name_description)

    @Composable
    override fun Content() {
        val userToAdd = LocalUserToAdd.current

        var isError by remember { mutableStateOf(false) }
        var name by remember {
            mutableStateOf(userToAdd.value.name.toTextFieldValue())
        }
        val defaultContainerColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(1.dp)

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(name) {
            isError = name.text.isEmpty()
            userToAdd.value = userToAdd.value.copy(
                name = name.text,
            )
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                    keyboardController?.hide()
                },
            ),
            isError = isError,
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.name),
                    color = LocalContentColor.current.copy(0.6f),
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = name.text.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = { name = "".toTextFieldValue() },
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(LocaleR.string.clear_text_button),
                            modifier = Modifier
                                .size(getAdaptiveDp(20.dp, 8.dp)),
                        )
                    }
                }
            },
            textStyle = MaterialTheme.typography.labelLarge
                .copy(
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    color = LocalContentColor.current.copy(0.6f),
                ).asAdaptiveTextStyle(
                    size = 16.sp,
                    increaseBy = 6.sp,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    getAdaptiveDp(
                        dp = 65.dp,
                        increaseBy = 18.dp,
                    ),
                ),
        )
    }
}

@Preview
@Composable
private fun NameScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
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
