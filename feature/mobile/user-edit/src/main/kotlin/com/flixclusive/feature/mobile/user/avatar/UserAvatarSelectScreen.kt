package com.flixclusive.feature.mobile.user.avatar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.AVATARS_IMAGE_COUNT
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun UserAvatarSelectScreen(
    resultNavigator: ResultBackNavigator<Int>
) {
    val avatarGridSize = 150.dp
    val columnsSize = getAdaptiveDp(
        dp = avatarGridSize,
        increaseBy = 70.dp
    )

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(LocaleR.string.choose_an_avatar),
                onNavigate = { resultNavigator.navigateBack(onlyIfResumed = true) }
            )
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(columnsSize),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(getAdaptiveDp(dp = 10.dp, increaseBy = 2.dp)),
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            items(AVATARS_IMAGE_COUNT) { i ->
                UserAvatar(
                    user = User(image = i),
                    shadowBlur = 0.dp,
                    borderWidth = 0.dp,
                    shadowSpread = 0.dp,
                    modifier = Modifier
                        .aspectRatio(1F)
                        .clickable {
                            resultNavigator.navigateBack(
                                result = i, onlyIfResumed = true
                            )
                        }
                )
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
                resultNavigator = object : ResultBackNavigator<Int> {
                    override fun navigateBack(result: Int, onlyIfResumed: Boolean) = Unit
                    override fun navigateBack(onlyIfResumed: Boolean) = Unit
                    override fun setResult(result: Int) = Unit
                }
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