package com.flixclusive.core.ui.mobile.component.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIconColor: Color = LocalContentColor.current,
    titleColor: Color = LocalContentColor.current,
    actionsColor: Color = LocalContentColor.current,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CommonTopBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIconColor = navigationIconColor,
        titleColor = titleColor,
        actionsColor = actionsColor,
        navigationIcon = {
            PlainTooltipBox(description = stringResource(LocaleR.string.navigate_up)) {
                ActionButton(onClick = onNavigate) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(LocaleR.string.navigate_up),
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style =
                    getAdaptiveTextStyle(
                        style = TypographyStyle.Body,
                        mode = TextStyleMode.Normal,
                        size = 20.sp,
                        increaseBy = 5.sp,
                    ).copy(fontWeight = FontWeight.SemiBold),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(start = 15.dp),
            )
        },
        actions = {
            actions?.invoke(this)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    navigationIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    navigationIconColor: Color = LocalContentColor.current,
    titleColor: Color = LocalContentColor.current,
    actionsColor: Color = LocalContentColor.current,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = getAdaptiveTopBarHeight(),
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                navigationIconContentColor = navigationIconColor,
                titleContentColor = titleColor,
                actionIconContentColor = actionsColor,
            ),
    )
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    size: Dp = getAdaptiveDp(35.dp),
    tint: Color = LocalContentColor.current,
    backgroundColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    color = backgroundColor,
                    alpha = if (enabled) 1f else 0.6f
                )
            }
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = size / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides tint, content = content)
    }
}

object CommonTopBarDefaults {
    val DefaultTopBarHeight = 65.dp
    private val DefaultTopBarHeightIncrementValue = 15.dp

    @Composable
    fun getAdaptiveTopBarHeight() =
        getAdaptiveDp(
            dp = DefaultTopBarHeight,
            increaseBy = DefaultTopBarHeightIncrementValue,
        )
}

@Preview
@Composable
private fun CommonTopBarBasePreview() {
    FlixclusiveTheme {
        Surface {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
            ) {
                CommonTopBar(
                    title = "Title",
                    onNavigate = {},
                    actions = {
                        Icon(Icons.Default.AccountBox, contentDescription = null)
                        Icon(Icons.Default.AccountBox, contentDescription = null)
                    },
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
