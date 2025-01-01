package com.flixclusive.feature.mobile.user.add.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.navigation.navigator.CommonUserEditNavigator
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.user.add.OnBoardingScreen
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class PinScreen(
    private val navigator: CommonUserEditNavigator
) : OnBoardingScreen {
    override val index: Int = 0
    override val title: UiText
        = UiText.StringResource(LocaleR.string.onboarding_profile_pin_title)
    override val description: UiText
        = UiText.StringResource(LocaleR.string.onboarding_profile_pin_description)
    override val canSkip = true

    @Composable
    override fun Content() {
        val surface = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        val shape = MaterialTheme.shapes.medium
        val horizontalPadding = getAdaptiveDp(8.dp)

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    getAdaptiveDp(
                        dp = 65.dp,
                        increaseBy = 18.dp
                    )
                )
                .clickable {
                    navigator.openUserPinSetupScreen()
                }
                .background(
                    color = surface,
                    shape = shape
                )
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.arrow_right_thin),
                contentDescription = stringResource(LocaleR.string.choose_an_avatar),
                modifier = Modifier
                    .padding(end = horizontalPadding)
                    .align(Alignment.CenterEnd)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(horizontalPadding * 2F),
                modifier = Modifier
                    .padding(start = horizontalPadding * 2F)
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.pin_lock),
                    contentDescription = stringResource(LocaleR.string.pin_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(),
                )

                Text(
                    text = stringResource(LocaleR.string.pin_setup),
                    style = getAdaptiveTextStyle(
                        mode = TextStyleMode.Emphasized,
                        size = 16.sp
                    )
                )
            }
        }
    }
}