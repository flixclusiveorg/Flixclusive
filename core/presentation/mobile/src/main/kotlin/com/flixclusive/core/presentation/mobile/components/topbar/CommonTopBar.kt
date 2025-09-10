package com.flixclusive.core.presentation.mobile.components.topbar

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
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.TypographySize
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarDefaults.getAdaptiveTopBarHeight
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

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
        navigationIcon = { DefaultNavigationIcon(onClick = onNavigate) },
        title = {
            Text(
                text = title,
                style = getTopBarHeadlinerTextStyle(),
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
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
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
        colors = CommonTopBarDefaults.colors(
            navigationIconColor = navigationIconColor,
            titleColor = titleColor,
            actionsColor = actionsColor,
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
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        color = backgroundColor,
                        alpha = if (enabled) 1f else 0.6f,
                    )
                }.clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication =
                        ripple(
                            bounded = false,
                            radius = size / 2,
                        ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            value = LocalContentColor provides tint.copy(if (enabled) 1f else 0.6f),
            content = content,
        )
    }
}

@Composable
fun DefaultNavigationIcon(onClick: () -> Unit) {
    PlainTooltipBox(description = stringResource(LocaleR.string.navigate_up)) {
        ActionButton(onClick = onClick) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.left_arrow),
                contentDescription = stringResource(LocaleR.string.navigate_up),
                dp = 16.dp,
                increaseBy = 3.dp,
            )
        }
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

    @Composable
    fun getTopBarHeadlinerTextStyle() =
        getAdaptiveTextStyle(
            style = AdaptiveTextStyle.Normal(TypographySize.Body),
            size = 20.sp,
            increaseBy = 5.sp,
        ).copy(fontWeight = FontWeight.SemiBold)

    @Composable
    fun colors(
        navigationIconColor: Color = LocalContentColor.current,
        titleColor: Color = LocalContentColor.current,
        actionsColor: Color = LocalContentColor.current,
    ) = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        navigationIconContentColor = navigationIconColor,
        titleContentColor = titleColor,
        actionIconContentColor = actionsColor,
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
