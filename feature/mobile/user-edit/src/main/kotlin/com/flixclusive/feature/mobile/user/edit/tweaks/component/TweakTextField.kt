package com.flixclusive.feature.mobile.user.edit.tweaks.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveModifierUtil.fillMaxAdaptiveWidth
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.toTextFieldValue
import com.flixclusive.feature.mobile.user.edit.tweaks.ProfileTweakUI
import com.flixclusive.feature.mobile.user.edit.tweaks.TweakUiUtil.DefaultShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private const val ON_NAME_CHANGE_DELAY = 800L

@Composable
internal fun TweakTextField(
    tweak: ProfileTweakUI.TextField
) {
    var value by remember { mutableStateOf(tweak.initialValue.toTextFieldValue()) }

    val changeCallback = onValueChangeWithDelay(tweak.onValueChange)

    val defaultContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = value,
        onValueChange = {
            value = it
            changeCallback(it.text)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus(true)
                keyboardController?.hide()
            }
        ),
        placeholder = {
            tweak.placeholder?.asString()?.let {
                Text(
                    text = it,
                    style = getAdaptiveTextStyle(
                        style = TypographyStyle.Label,
                        mode = TextStyleMode.Emphasized,
                    ).copy(
                        color = LocalContentColor.current.onMediumEmphasis(),
                    )
                )
            }
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = value.text.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                IconButton(
                    onClick = { value = "".toTextFieldValue() }
                ) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.outline_close_square),
                        contentDescription = stringResource(LocaleR.string.clear_text_button)
                    )
                }
            }
        },
        textStyle = getAdaptiveTextStyle(
            size = 16.sp,
            style = TypographyStyle.Body,
            mode = TextStyleMode.Normal
        ).copy(
            textAlign = TextAlign.Start
        ),
        singleLine = true,
        shape = DefaultShape,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = defaultContainerColor,
            focusedContainerColor = defaultContainerColor,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxAdaptiveWidth()
            .height(
                getAdaptiveDp(
                    dp = 65.dp,
                    increaseBy = 15.dp
                )
            )
    )
}

@Composable
private fun onValueChangeWithDelay(
    onValueChange: (String) -> Unit,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    return fun(newValue: String) {
        if (job?.isActive == true) {
            job?.cancel()
        }

        job = scope.launch {
            delay(ON_NAME_CHANGE_DELAY)
            if (newValue.isNotEmpty()) {
                onValueChange(newValue)
            }
        }
    }
}
