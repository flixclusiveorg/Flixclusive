package com.flixclusive.feature.mobile.user.pin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.user.pin.component.DEFAULT_DELAY
import com.flixclusive.feature.mobile.user.pin.component.HeaderLabel
import com.flixclusive.feature.mobile.user.pin.component.PinScreenDefault
import com.flixclusive.feature.mobile.user.pin.component.PinSetupScreenCompactLandscape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import kotlinx.coroutines.delay
import com.flixclusive.core.strings.R as LocaleR

@Destination
@Composable
internal fun PinVerifyScreen(
    actualPin: String,
    resultNavigator: ResultBackNavigator<PinVerificationResult>,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass

    val pinToVerify = rememberSaveable { mutableStateOf<String>("") }

    val isTyping = rememberSaveable { mutableStateOf(false) }
    val hasErrors = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isTyping) {
        delay(DEFAULT_DELAY)
        isTyping.value = false
    }

    LaunchedEffect(hasErrors) {
        delay(DEFAULT_DELAY)
        hasErrors.value = false
    }

    val onBack = fun() {
        resultNavigator.navigateBack(
            onlyIfResumed = true,
            result = PinVerificationResult(isVerified = false),
        )
    }

    val onConfirm = fun() {
        if (pinToVerify.value == actualPin) {
            resultNavigator.navigateBack(
                onlyIfResumed = true,
                result = PinVerificationResult(isVerified = true),
            )
        } else {
            hasErrors.value = true
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = getAdaptiveDp(DefaultScreenPaddingHorizontal, 2.dp)),
    ) {
        if (windowSizeClass.isCompact) {
            PinVerifyScreenCompactLandscape(
                pin = pinToVerify,
                isTyping = isTyping,
                hasErrors = hasErrors,
                onBack = onBack,
                onConfirm = onConfirm,
            )
        } else {
            PinVerifyScreenDefault(
                pin = pinToVerify,
                isTyping = isTyping,
                hasErrors = hasErrors,
                onBack = onBack,
                onConfirm = onConfirm,
            )
        }
    }
}

@Suppress("ktlint:compose:mutable-state-param-check")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinVerifyScreenCompactLandscape(
    pin: MutableState<String>,
    isTyping: MutableState<Boolean>,
    hasErrors: MutableState<Boolean>,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    PinSetupScreenCompactLandscape(
        pin = pin,
        isTyping = isTyping,
        hasErrors = hasErrors,
        onBack = onBack,
        onConfirm = onConfirm,
    ) {
        HeaderLabel(
            title = stringResource(LocaleR.string.pin_verify),
        )
    }
}

@Suppress("ktlint:compose:mutable-state-param-check")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinVerifyScreenDefault(
    pin: MutableState<String>,
    isTyping: MutableState<Boolean>,
    hasErrors: MutableState<Boolean>,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    PinScreenDefault(
        pin = pin,
        isTyping = isTyping,
        hasErrors = hasErrors,
        onBack = onBack,
        onConfirm = onConfirm,
    ) {
        HeaderLabel(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally),
            title = stringResource(LocaleR.string.pin_verify),
        )
    }
}

@Preview
@Composable
private fun PinVerifyScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            PinVerifyScreen(
                actualPin = "0000",
                resultNavigator = object : ResultBackNavigator<PinVerificationResult> {
                    override fun navigateBack(
                        result: PinVerificationResult,
                        onlyIfResumed: Boolean,
                    ) = Unit

                    override fun navigateBack(onlyIfResumed: Boolean) = Unit

                    override fun setResult(result: PinVerificationResult) = Unit
                },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PinVerifyScreenCompactLandscapePreview() {
    PinVerifyScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PinVerifyScreenMediumPortraitPreview() {
    PinVerifyScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PinVerifyScreenMediumLandscapePreview() {
    PinVerifyScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PinVerifyScreenExtendedPortraitPreview() {
    PinVerifyScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PinVerifyScreenExtendedLandscapePreview() {
    PinVerifyScreenBasePreview()
}
