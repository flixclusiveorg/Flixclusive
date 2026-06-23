package com.flixclusive.feature.mobile.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.TopAppBarLayout
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal val HomeScreenTopBarDefaultHeight = 64.dp // not the actual expanded height

@Composable
internal fun HomeScreenTopBar(
    title: String,
    containerAlpha: () -> Float,
    onFilterClick: () -> Unit,
    enableFilterButton: () -> Boolean,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = HomeScreenTopBarDefaultHeight,
) {
    val appBarContainerColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .drawBehind {
                drawRect(
                    color = appBarContainerColor,
                    alpha = containerAlpha(),
                )
            }.windowInsetsPadding(WindowInsets.statusBars),
        color = Color.Transparent
    ) {
        TopAppBarLayout(
            modifier = Modifier
                .clipToBounds()
                .heightIn(max = expandedHeight)
                .padding(horizontal = 8.dp),
            scrolledOffset = { 0f },
            navigationIconContentColor = LocalContentColor.current,
            titleContentColor = LocalContentColor.current,
            actionIconContentColor = LocalContentColor.current,
            title = {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            titleTextStyle = MaterialTheme.typography.titleLarge
                .copy(fontWeight = FontWeight.Black)
                .asAdaptiveTextStyle(
                    size = 24.sp,
                    increaseBy = 5.sp,
                ),
            hideTitleSemantics = false,
            navigationIcon = {},
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                        ActionButton(
                            onClick = onFilterClick,
                            enabled = enableFilterButton()
                        ) {
                            Icon(
                                painter = painterResource(UiCommonR.drawable.filter_list),
                                contentDescription = stringResource(LocaleR.string.filter_button),
                            )
                        }
                    }
                }
            },
        )
    }
}
