package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.topbar.TopAppBarLayout
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun FilmScreenTopBar(
    title: String,
    containerAlpha: () -> Float,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = 64.dp,
) {
    val appBarContainerColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .drawBehind {
                drawRect(
                    color = appBarContainerColor,
                    alpha = containerAlpha()
                )
            },
        color = Color.Transparent
    ) {
        TopAppBarLayout(
            modifier = Modifier
                .clipToBounds()
                .heightIn(max = expandedHeight),
            scrolledOffset = { 0f },
            navigationIconContentColor = LocalContentColor.current,
            titleContentColor = LocalContentColor.current,
            actionIconContentColor = LocalContentColor.current,
            title = {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .graphicsLayer {
                            alpha = containerAlpha()
                        },
                )
            },
            titleTextStyle = getTopBarHeadlinerTextStyle(),
            hideTitleSemantics = false,
            navigationIcon = {
                PlainTooltipBox(description = stringResource(LocaleR.string.navigate_up)) {
                    IconButton(
                        onClick = onNavigate,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .drawBehind {
                                val alpha = 0.6f - containerAlpha().coerceIn(0f, 0.6f)

                                drawRect(
                                    color = appBarContainerColor,
                                    alpha = alpha,
                                )
                            }
                    ) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.left_arrow),
                            contentDescription = stringResource(LocaleR.string.navigate_up),
                            dp = 16.dp,
                            increaseBy = 3.dp,
                        )
                    }
                }
            },
            actions = {},
        )
    }
}
