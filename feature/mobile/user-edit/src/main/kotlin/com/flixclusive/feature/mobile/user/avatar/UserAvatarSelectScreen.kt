package com.flixclusive.feature.mobile.user.avatar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyUser
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Destination
@Composable
internal fun UserAvatarSelectScreen(
    selected: Int,
    resultNavigator: ResultBackNavigator<Int>,
) {
    val surface = MaterialTheme.colorScheme.surface
    val cornerRadius = 8F
    val avatarGridSize = 150.dp
    val columnsSize =
        getAdaptiveDp(
            dp = avatarGridSize,
            increaseBy = 70.dp,
        )

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(LocaleR.string.choose_an_avatar),
                onNavigate = { resultNavigator.navigateBack(onlyIfResumed = true) },
            )
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(columnsSize),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(getAdaptiveDp(dp = 10.dp, increaseBy = 2.dp)),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(it),
        ) {
            items(AVATARS_IMAGE_COUNT) { i ->
                Box {
                    UserAvatar(
                        user = getDummyUser(image = i),
                        shadowBlur = 0.dp,
                        borderWidth = 0.dp,
                        shadowSpread = 0.dp,
                        modifier =
                            Modifier
                                .aspectRatio(1F)
                                .clickable {
                                    resultNavigator.navigateBack(
                                        result = i,
                                        onlyIfResumed = true,
                                    )
                                },
                    )

                    if (selected == i) {
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .drawBehind {
                                        drawRoundRect(
                                            Brush.verticalGradient(
                                                0F to Color.Transparent,
                                                1F to surface,
                                            ),
                                            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                        )
                                    },
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(
                                        bottom = getAdaptiveDp(dp = 10.dp, increaseBy = 2.dp),
                                        start = getAdaptiveDp(dp = 10.dp, increaseBy = 2.dp),
                                    ),
                        ) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.check),
                                contentDescription = stringResource(LocaleR.string.selected_avatar_content_desc),
                                increaseBy = 4.dp,
                            )

                            Text(
                                text = stringResource(LocaleR.string.selected_label),
                                style =
                                    getAdaptiveTextStyle(
                                        mode = TextStyleMode.Normal,
                                        style = TypographyStyle.Title,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UserAvatarSelectScreenBasePreview() {
    FlixclusiveTheme {
        Surface {
            UserAvatarSelectScreen(
                selected = 1,
                resultNavigator =
                    object : ResultBackNavigator<Int> {
                        override fun navigateBack(
                            result: Int,
                            onlyIfResumed: Boolean,
                        ) = Unit

                        override fun navigateBack(onlyIfResumed: Boolean) = Unit

                        override fun setResult(result: Int) = Unit
                    },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun UserAvatarSelectScreenCompactLandscapePreview() {
    UserAvatarSelectScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun UserAvatarSelectScreenMediumPortraitPreview() {
    UserAvatarSelectScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun UserAvatarSelectScreenMediumLandscapePreview() {
    UserAvatarSelectScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun UserAvatarSelectScreenExtendedPortraitPreview() {
    UserAvatarSelectScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun UserAvatarSelectScreenExtendedLandscapePreview() {
    UserAvatarSelectScreenBasePreview()
}
