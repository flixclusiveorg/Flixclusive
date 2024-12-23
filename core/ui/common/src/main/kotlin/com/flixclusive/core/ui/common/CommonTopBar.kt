package com.flixclusive.core.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveTextUnit
import com.flixclusive.core.locale.R as LocaleR

@Composable
fun CommonTopBar(
    modifier: Modifier = Modifier,
    title: String,
    onNavigate: () -> Unit,
    endContent: (@Composable () -> Unit)? = null,
) {
    CommonTopBar(
        modifier = modifier,
        navigationIcon = {
            val iconSize = getAdaptiveDp(16.dp, 3.dp)
            IconButton(onClick = onNavigate) {
                Icon(
                    painter = painterResource(R.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up),
                    modifier = Modifier
                        .size(iconSize)
                )
            }
        },
        body = {
            val fontSize = getAdaptiveTextUnit(20.sp, 5)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 15.dp)
            )
        },
        actions = {
            endContent?.invoke()
        }
    )
}

@Composable
fun CommonTopBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable RowScope.() -> Unit,
    body: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = modifier
                .height(getAdaptiveTopBarHeight())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon()
            body()
            Spacer(modifier = Modifier.weight(1F))
            actions()
        }
    }
}

object CommonTopBarDefaults {
    val DefaultTopBarHeight = 65.dp
    private val DefaultTopBarHeightIncrementValue = 15.dp

    @Composable
    fun getAdaptiveTopBarHeight()
        = getAdaptiveDp(
            dp = DefaultTopBarHeight,
            increaseBy = DefaultTopBarHeightIncrementValue
        )
}

@Preview
@Composable
private fun CommonTopBarBasePreview() {
    FlixclusiveTheme {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.onSurface
                    )
            ) {
                CommonTopBar(
                    title = "Title",
                    onNavigate = {}
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun CommonTopBarCompactLandscapePreview() {
    CommonTopBarBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun CommonTopBarMediumPortraitPreview() {
    CommonTopBarBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun CommonTopBarMediumLandscapePreview() {
    CommonTopBarBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun CommonTopBarExtendedPortraitPreview() {
    CommonTopBarBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun CommonTopBarExtendedLandscapePreview() {
    CommonTopBarBasePreview()
}
