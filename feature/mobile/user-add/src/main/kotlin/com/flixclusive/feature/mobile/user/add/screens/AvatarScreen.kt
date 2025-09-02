package com.flixclusive.feature.mobile.user.add.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveTextStyle
import com.flixclusive.feature.mobile.user.add.AddUserScreenNavigator
import com.flixclusive.feature.mobile.user.add.OnBoardingScreen
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.LocalUserToAdd
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class AvatarScreen(
    private val navigator: AddUserScreenNavigator
) : OnBoardingScreen {
    override val index: Int = 0
    override val title: UiText
        = UiText.StringResource(LocaleR.string.onboarding_profile_avatar_title)
    override val description: UiText
        = UiText.StringResource(LocaleR.string.onboarding_profile_avatar_description)

    @Composable
    override fun Content() {
        val user = LocalUserToAdd.current.value
        val context = LocalContext.current
        val avatarId = remember(user.image) {
            context.getAvatarResource(user.image)
        }

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
                .background(
                    color = surface,
                    shape = shape
                )
                .graphicsLayer {
                    clip = true
                    this.shape = shape
                }
                .clickable {
                    navigator.openUserAvatarSelectScreen(user.image)
                }
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.horizontalGradient(
                                0F to surface,
                                0.4F to surface.copy(0.8F),
                                1F to Color.Transparent
                            )
                        )
                    }
            ) {
                Image(
                    painter = painterResource(avatarId),
                    contentDescription = stringResource(LocaleR.string.avatar_chosen_content_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Icon(
                painter = painterResource(UiCommonR.drawable.arrow_right_thin),
                contentDescription = stringResource(LocaleR.string.choose_an_avatar),
                modifier = Modifier
                    .padding(end = horizontalPadding)
                    .align(Alignment.CenterEnd)
            )

            Text(
                text = stringResource(LocaleR.string.avatar_number, user.image + 1),
                style = getAdaptiveTextStyle(
                    style = AdaptiveTextStyle.Emphasized,
                    size = 16.sp
                ),
                modifier = Modifier
                    .padding(start = horizontalPadding * 2F)
            )
        }
    }
}
