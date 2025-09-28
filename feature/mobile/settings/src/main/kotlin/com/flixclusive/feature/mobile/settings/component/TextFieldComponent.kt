package com.flixclusive.feature.mobile.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.clearFocusOnSoftKeyboardHide
import com.flixclusive.core.presentation.common.extensions.showSoftKeyboard
import com.flixclusive.core.presentation.common.extensions.toTextFieldValue
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun TextFieldComponent(
    title: String,
    valueProvider: () -> String,
    modifier: Modifier = Modifier,
    descriptionProvider: (() -> String)? = null,
    enabledProvider: () -> Boolean = { true },
    icon: Painter? = null,
    onValueChange: (String) -> Unit,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    val onDismissRequest = fun() {
        isDialogShown = false
    }

    ClickableComponent(
        modifier = modifier,
        title = title,
        descriptionProvider = descriptionProvider,
        enabledProvider = enabledProvider,
        icon = icon,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        var currentValue by remember { mutableStateOf(valueProvider().toTextFieldValue()) }

        BaseTweakDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            onConfirm =
                if (currentValue.text != valueProvider() && currentValue.text.isNotBlank()) {
                    fun() {
                        onValueChange(currentValue.text)
                    }
                } else {
                    null
                },
        ) {
            val defaultContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

            TextField(
                value = currentValue,
                onValueChange = { currentValue = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                isError = currentValue.text.isEmpty(),
                trailingIcon = {
                    AnimatedVisibility(
                        visible = currentValue.text.isNotEmpty(),
                        enter = scaleIn(),
                        exit = scaleOut(),
                    ) {
                        IconButton(onClick = { currentValue = "".toTextFieldValue() }) {
                            Icon(
                                painter = painterResource(UiCommonR.drawable.outline_close_square),
                                contentDescription = stringResource(LocaleR.string.clear_text_button),
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge
                    .copy(textAlign = TextAlign.Start)
                    .asAdaptiveTextStyle(),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors =
                    TextFieldDefaults.colors(
                        unfocusedContainerColor = defaultContainerColor,
                        focusedContainerColor = defaultContainerColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier
                    .showSoftKeyboard(true)
                    .clearFocusOnSoftKeyboardHide()
                    .fillMaxWidth()
                    .height(
                        getAdaptiveDp(
                            dp = 65.dp,
                            increaseBy = 15.dp,
                        ),
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun TextFieldComponentBasePreview() {
    var value by remember { mutableStateOf("https://example.com") }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                TextFieldComponent(
                    title = "TextField tweak with icon",
                    descriptionProvider = { "TextField tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    valueProvider = { value },
                    onValueChange = { value = it },
                )

                TextFieldComponent(
                    title = "TextField tweak",
                    descriptionProvider = { "TextField tweak summary" },
                    valueProvider = { value },
                    onValueChange = { value = it },
                )

                TextFieldComponent(
                    title = "TextField tweak no summary",
                    valueProvider = { value },
                    onValueChange = { value = it },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun TextFieldComponentCompactLandscapePreview() {
    TextFieldComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun TextFieldComponentMediumPortraitPreview() {
    TextFieldComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun TextFieldComponentMediumLandscapePreview() {
    TextFieldComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun TextFieldComponentExtendedPortraitPreview() {
    TextFieldComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun TextFieldComponentExtendedLandscapePreview() {
    TextFieldComponentBasePreview()
}
