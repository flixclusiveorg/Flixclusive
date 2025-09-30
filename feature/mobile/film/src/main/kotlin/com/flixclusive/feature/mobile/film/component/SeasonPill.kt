package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun SeasonPill(
    season: Season,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = remember(season) {
        season.name.ifEmpty {
            context.getString(LocaleR.string.untitled_season, season.number)
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "SeasonPillContainerColorAnimation",
    )

    val border = when {
        selected -> null
        else -> BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
        )
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = border,
        contentColor = contentColorFor(containerColor),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium
                .copy(
                    fontWeight = when (selected) {
                        true -> FontWeight.Bold
                        false -> FontWeight.Medium
                    },
                ).asAdaptiveTextStyle(),
            color = LocalContentColor.current.copy(
                alpha = when (selected) {
                    true -> 1f
                    false -> 0.7f
                },
            ),
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 5.dp,
            ),
        )
    }
}

@Preview
@Composable
private fun SeasonPillPreview() {
    FlixclusiveTheme {
        Surface {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(20) {
                    SeasonPill(
                        season = Season(number = it, name = "Season ${it + 1}"),
                        selected = it == 2,
                        onClick = {},
                    )
                }
            }
        }
    }
}
