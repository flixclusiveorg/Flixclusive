package com.flixclusive.feature.mobile.user.pin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.navigation.navargs.PinWithHintResult
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveModifierUtil.fillMaxAdaptiveWidth
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.isCompact
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.util.ComposeUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.user.edit.tweaks.TweakUiUtil.DefaultShape
import com.flixclusive.model.database.MAX_USER_PIN_LENGTH
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import kotlinx.coroutines.delay
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private const val MAX_NUMBER_LENGTH = 10
private const val DEFAULT_DELAY = 2000L

private enum class PinSetupStep {
    Verify,
    Setup,
    Confirm,
    Hint,
}

@Destination
@Composable
internal fun PinSetupScreen(
    currentPin: String? = null,
    isRemovingPin: Boolean = false,
    resultNavigator: ResultBackNavigator<PinWithHintResult>,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass

    val newPin = rememberSaveable { mutableStateOf("") }
    val pinHint = rememberSaveable { mutableStateOf("") }
    var pinToConfirm by rememberSaveable { mutableStateOf<String?>(null) }

    val isTyping = rememberSaveable { mutableStateOf(false) }
    val hasErrors = rememberSaveable { mutableStateOf(false) }

    var stepState by rememberSaveable {
        val state = if (currentPin != null) PinSetupStep.Verify else PinSetupStep.Setup
        mutableStateOf(state)
    }

    LaunchedEffect(isTyping) {
        delay(DEFAULT_DELAY)
        isTyping.value = false
    }

    LaunchedEffect(hasErrors) {
        delay(DEFAULT_DELAY)
        hasErrors.value = false
    }

    val onConfirm = fun () {
        val isCurrentPinVerificationCorrect = stepState == PinSetupStep.Verify && newPin.value == currentPin
        val isNewPinSame = stepState == PinSetupStep.Confirm && newPin.value == pinToConfirm

        if (isCurrentPinVerificationCorrect && isRemovingPin) {
            resultNavigator.navigateBack(
                onlyIfResumed = true,
                result =
                    PinWithHintResult(
                        pin = null,
                        pinHint = null,
                    ),
            )
        } else if (isCurrentPinVerificationCorrect) {
            newPin.value = ""
            stepState = PinSetupStep.Setup
        } else if (stepState == PinSetupStep.Setup) {
            pinToConfirm = newPin.value
            newPin.value = ""
            stepState = PinSetupStep.Confirm
        } else if (isNewPinSame) {
            stepState = PinSetupStep.Hint
        } else if (stepState == PinSetupStep.Hint && pinHint.value.isNotEmpty()) {
            resultNavigator.navigateBack(
                onlyIfResumed = true,
                result =
                    PinWithHintResult(
                        pin = newPin.value,
                        pinHint = pinHint.value,
                    ),
            )
        } else if (stepState == PinSetupStep.Confirm ||
            stepState == PinSetupStep.Verify ||
            stepState == PinSetupStep.Hint
        ) {
            hasErrors.value = true
        }
    }

    val onBack = {
        if (stepState == PinSetupStep.Verify || stepState == PinSetupStep.Setup) {
            resultNavigator.navigateBack(onlyIfResumed = true)
        } else if (stepState == PinSetupStep.Confirm || stepState == PinSetupStep.Hint) {
            newPin.value = ""
            stepState = PinSetupStep.Setup
        }
    }

    AnimatedContent(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = getAdaptiveDp(DefaultScreenPaddingHorizontal, 2.dp)),
        targetState = stepState == PinSetupStep.Hint,
        label = "PinSetupStep",
        transitionSpec = {
            fadeIn(tween(durationMillis = 500)) togetherWith fadeOut()
        },
    ) { isOnHintStep ->
        if (isOnHintStep) {
            PinSetupHintScreen(
                pinHint = pinHint,
                onConfirm = onConfirm,
                onSkip = {
                    resultNavigator.navigateBack(
                        onlyIfResumed = true,
                        result =
                            PinWithHintResult(
                                pin = newPin.value,
                                pinHint = null,
                            ),
                    )
                },
            )
        } else if (windowSizeClass.isCompact) {
            PinSetupScreenCompactLandscape(
                currentPin = currentPin,
                pin = newPin,
                isTyping = isTyping,
                hasErrors = hasErrors,
                stepState = stepState,
                onBack = onBack,
                onConfirm = onConfirm,
            )
        } else {
            PinSetupScreenDefault(
                currentPin = currentPin,
                pin = newPin,
                isTyping = isTyping,
                hasErrors = hasErrors,
                stepState = stepState,
                onBack = onBack,
                onConfirm = onConfirm,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinSetupScreenCompactLandscape(
    currentPin: String? = null,
    pin: MutableState<String>,
    isTyping: MutableState<Boolean>,
    hasErrors: MutableState<Boolean>,
    stepState: PinSetupStep,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        CommonTopBar(
            title = "",
            onNavigate = onBack,
            rowModifier =
                Modifier
                    .align(Alignment.TopCenter),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth(0.8F)
                    .align(Alignment.Center)
                    .scale(0.85F),
        ) {
            Column(
                modifier = Modifier.weight(0.5F),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = 25.dp,
                        alignment = Alignment.CenterVertically,
                    ),
            ) {
                AnimatedContent(
                    targetState = stepState,
                    label = "PinSetupStep",
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 500)) togetherWith fadeOut()
                    },
                ) {
                    val title =
                        when (it) {
                            PinSetupStep.Verify -> stringResource(LocaleR.string.pin_verify)
                            PinSetupStep.Setup -> stringResource(LocaleR.string.pin_setup)
                            PinSetupStep.Confirm -> stringResource(LocaleR.string.pin_confirm)
                            else -> null
                        }

                    if (title != null) {
                        HeaderLabel(title = title)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    repeat(MAX_USER_PIN_LENGTH) {
                        PinPlaceholder(
                            showPin = it == pin.value.length - 1 && isTyping.value,
                            hasErrors = hasErrors.value,
                            char = pin.value.getOrNull(it),
                        )
                    }
                }
            }

            val pinPadding = getAdaptiveDp(20.dp)

            FlowRow(
                modifier = Modifier.weight(0.5F),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = pinPadding,
                        alignment = Alignment.CenterHorizontally,
                    ),
                verticalArrangement = Arrangement.spacedBy(pinPadding),
                maxItemsInEachRow = 3,
            ) {
                repeat(MAX_NUMBER_LENGTH + 2) {
                    when (val digit = it + 1) {
                        10 -> {
                            PinButton(
                                enabled = pin.value.isNotEmpty(),
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.isNotEmpty()) {
                                        isTyping.value = false
                                        pin.value = pin.value.dropLast(1)
                                    }
                                },
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.backspace_filled),
                                    contentDescription = stringResource(LocaleR.string.backspace_content_desc),
                                )
                            }
                        }
                        MAX_NUMBER_LENGTH + 2 -> {
                            PinButton(
                                enabled = pin.value.length == MAX_USER_PIN_LENGTH,
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.length == MAX_USER_PIN_LENGTH) {
                                        onConfirm()
                                    }
                                },
                            ) {
                                Text(
                                    text = stringResource(LocaleR.string.ok),
                                    style =
                                        getAdaptiveTextStyle(
                                            style = TypographyStyle.Title,
                                            mode = TextStyleMode.Emphasized,
                                        ),
                                )
                            }
                        }
                        else -> {
                            val coercedDigit = digit.coerceAtMost(MAX_NUMBER_LENGTH)
                            PinButton(
                                digit = coercedDigit % MAX_NUMBER_LENGTH,
                                onClick = {
                                    if (pin.value.length < MAX_USER_PIN_LENGTH) {
                                        isTyping.value = true
                                        hasErrors.value = false
                                        pin.value += "${coercedDigit % MAX_NUMBER_LENGTH}"
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinSetupScreenDefault(
    currentPin: String? = null,
    pin: MutableState<String>,
    isTyping: MutableState<Boolean>,
    hasErrors: MutableState<Boolean>,
    stepState: PinSetupStep,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    Scaffold(
        topBar = {
            CommonTopBar(
                title = "",
                onNavigate = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    space = 25.dp,
                    alignment = Alignment.CenterVertically,
                ),
        ) {
            AnimatedContent(
                targetState = stepState,
                label = "PinSetupStep",
                transitionSpec = {
                    fadeIn(tween(durationMillis = 500)) togetherWith fadeOut()
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally),
            ) {
                val title =
                    when (it) {
                        PinSetupStep.Verify -> stringResource(LocaleR.string.pin_verify)
                        PinSetupStep.Setup -> stringResource(LocaleR.string.pin_setup)
                        PinSetupStep.Confirm -> stringResource(LocaleR.string.pin_confirm)
                        else -> null
                    }

                if (title != null) {
                    HeaderLabel(title = title)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 25.dp),
            ) {
                repeat(MAX_USER_PIN_LENGTH) {
                    PinPlaceholder(
                        showPin = it == pin.value.length - 1 && isTyping.value,
                        hasErrors = hasErrors.value,
                        char = pin.value.getOrNull(it),
                    )
                }
            }

            val pinPadding = getAdaptiveDp(20.dp)

            FlowRow(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = pinPadding,
                        alignment = Alignment.CenterHorizontally,
                    ),
                verticalArrangement = Arrangement.spacedBy(pinPadding),
                maxItemsInEachRow = 3,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally),
            ) {
                repeat(MAX_NUMBER_LENGTH + 2) {
                    when (val digit = it + 1) {
                        10 -> {
                            PinButton(
                                enabled = pin.value.isNotEmpty(),
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.isNotEmpty()) {
                                        isTyping.value = false
                                        pin.value = pin.value.dropLast(1)
                                    }
                                },
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.backspace_filled),
                                    contentDescription = stringResource(LocaleR.string.backspace_content_desc),
                                )
                            }
                        }
                        MAX_NUMBER_LENGTH + 2 -> {
                            PinButton(
                                enabled = pin.value.length == MAX_USER_PIN_LENGTH,
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.length == MAX_USER_PIN_LENGTH) {
                                        onConfirm()
                                    }
                                },
                            ) {
                                Text(
                                    text = stringResource(LocaleR.string.ok),
                                    style =
                                        getAdaptiveTextStyle(
                                            style = TypographyStyle.Title,
                                            mode = TextStyleMode.Emphasized,
                                        ),
                                )
                            }
                        }
                        else -> {
                            val coercedDigit = digit.coerceAtMost(MAX_NUMBER_LENGTH)
                            PinButton(
                                digit = coercedDigit % MAX_NUMBER_LENGTH,
                                onClick = {
                                    if (pin.value.length < MAX_USER_PIN_LENGTH) {
                                        isTyping.value = true
                                        hasErrors.value = false
                                        pin.value += "${coercedDigit % MAX_NUMBER_LENGTH}"
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinSetupHintScreen(
    pinHint: MutableState<String>,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
) {
    val defaultContainerColor =
        MaterialTheme.colorScheme
            .surfaceColorAtElevation(1.dp)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val adaptiveMaxWidth =
        Modifier
            .fillMaxAdaptiveWidth(
                compact = 1F,
                medium = 0.8F,
                expanded = 0.5F,
            )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .noIndicationClickable {
                    focusManager.clearFocus(true)
                    keyboardController?.hide()
                },
        verticalArrangement =
            Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderLabel(
            title = stringResource(LocaleR.string.pin_hint_description),
        )

        Text(
            text = stringResource(LocaleR.string.pin_hint_sub_description),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8F),
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Body,
                    mode = TextStyleMode.NonEmphasized,
                    size = 14.sp,
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = pinHint.value,
            onValueChange = { pinHint.value = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        focusManager.clearFocus(true)
                        keyboardController?.hide()
                    },
                ),
            placeholder = {
                Text(
                    text = stringResource(LocaleR.string.pin_hint),
                    style =
                        getAdaptiveTextStyle(
                            style = TypographyStyle.Label,
                            mode = TextStyleMode.Emphasized,
                        ).copy(
                            color = LocalContentColor.current.onMediumEmphasis(),
                        ),
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = pinHint.value.isNotEmpty(),
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(
                        onClick = { pinHint.value = "" },
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.outline_close_square),
                            contentDescription = stringResource(LocaleR.string.clear_text_button),
                        )
                    }
                }
            },
            textStyle =
                getAdaptiveTextStyle(
                    size = 16.sp,
                    style = TypographyStyle.Body,
                    mode = TextStyleMode.Normal,
                ).copy(
                    textAlign = TextAlign.Start,
                ),
            singleLine = true,
            shape = DefaultShape,
            colors =
                TextFieldDefaults.colors(
                    unfocusedContainerColor = defaultContainerColor,
                    focusedContainerColor = defaultContainerColor,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                ),
            modifier =
                adaptiveMaxWidth
                    .height(
                        getAdaptiveDp(
                            dp = 65.dp,
                            increaseBy = 15.dp,
                        ),
                    ),
        )

        val adaptiveHeight = Modifier.height(getAdaptiveDp(45.dp, 10.dp))

        Row(
            modifier = adaptiveMaxWidth,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                onClick = onSkip,
                shape = MaterialTheme.shapes.small,
                modifier =
                    adaptiveHeight
                        .weight(0.5F),
            ) {
                Text(
                    text = stringResource(LocaleR.string.skip),
                    style =
                        getAdaptiveTextStyle(
                            style = TypographyStyle.Label,
                            mode = TextStyleMode.NonEmphasized,
                        ).copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
            }

            FilledTonalButton(
                onClick = onConfirm,
                enabled = pinHint.value.isNotEmpty(),
                shape = MaterialTheme.shapes.small,
                modifier =
                    adaptiveHeight
                        .weight(0.5F),
            ) {
                Text(
                    text = stringResource(LocaleR.string.finish),
                    style =
                        getAdaptiveTextStyle(
                            style = TypographyStyle.Label,
                            mode = TextStyleMode.Emphasized,
                            increaseBy = 6.sp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun PinPlaceholder(
    showPin: Boolean,
    hasErrors: Boolean,
    char: Char?,
) {
    val size = getAdaptiveDp(16.dp)

    val inputColor by animateColorAsState(
        label = "PinPlaceholderInputColor",
        targetValue =
            if (char != null && hasErrors) {
                MaterialTheme.colorScheme.error
            } else if (char != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(20.dp)
            },
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                space = 5.dp,
                alignment = Alignment.Bottom,
            ),
        modifier =
            Modifier
                .heightIn(size * 4F),
    ) {
        AnimatedVisibility(
            visible = showPin,
            enter = fadeIn() + slideInVertically(tween(100)) { it / 4 },
            exit = fadeOut(),
        ) {
            Text(
                text = "${char ?: ""}",
                style =
                    getAdaptiveTextStyle(
                        style = TypographyStyle.Title,
                        mode = TextStyleMode.Emphasized,
                        size = 25.sp,
                    ),
            )
        }

        Spacer(
            modifier =
                Modifier
                    .size(size)
                    .background(
                        color = inputColor,
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun PinButton(
    digit: Int,
    onClick: () -> Unit,
) {
    PinButton(
        onClick = onClick,
    ) {
        Text(
            text = "$digit",
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Title,
                    mode = TextStyleMode.Emphasized,
                ),
        )
    }
}

@Composable
private fun HeaderLabel(title: String) {
    Text(
        text = title,
        style =
            getAdaptiveTextStyle(
                style = TypographyStyle.Title,
                mode = TextStyleMode.Emphasized,
                size = 25.sp,
            ),
    )
}

@Composable
private fun PinButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    noEmphasis: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (noEmphasis) {
        TextButton(
            enabled = enabled,
            content = content,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(0.dp),
            modifier =
                Modifier
                    .size(getAdaptiveDp(65.dp)),
        )
    } else {
        OutlinedButton(
            enabled = enabled,
            content = content,
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(0.dp),
            modifier =
                Modifier
                    .size(getAdaptiveDp(65.dp)),
        )
    }
}

@Preview
@Composable
private fun PinSetupScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            PinSetupScreen(
                currentPin = "0000",
                resultNavigator =
                    object : ResultBackNavigator<PinWithHintResult> {
                        override fun navigateBack(
                            result: PinWithHintResult,
                            onlyIfResumed: Boolean,
                        ) = Unit

                        override fun navigateBack(onlyIfResumed: Boolean) = Unit

                        override fun setResult(result: PinWithHintResult) = Unit
                    },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PinSetupScreenCompactLandscapePreview() {
    PinSetupScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PinSetupScreenMediumPortraitPreview() {
    PinSetupScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PinSetupScreenMediumLandscapePreview() {
    PinSetupScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PinSetupScreenExtendedPortraitPreview() {
    PinSetupScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PinSetupScreenExtendedLandscapePreview() {
    PinSetupScreenBasePreview()
}
