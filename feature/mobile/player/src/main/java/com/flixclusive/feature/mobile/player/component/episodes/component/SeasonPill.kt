package com.flixclusive.feature.mobile.player.component.episodes.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.film.common.tv.Season

@Composable
internal fun SeasonPill(
    modifier: Modifier = Modifier,
    season: Season,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.surface else LocalContentColor.current.copy(0.6f))
    val containerColor by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
    val scale = animateFloatAsState(targetValue = if (selected) 1.1F else 1F)
    val alpha = animateFloatAsState(targetValue = if (selected) 1F else 0.8F)

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scale.value
                this.scaleY = scale.value

                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            enabled = !selected,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = containerColor,
                disabledContainerColor = containerColor,
                contentColor = contentColor,
                disabledContentColor = contentColor,
            ),
            onClick = onClick,
            contentPadding = PaddingValues(
                horizontal = 10.dp,
                vertical = 3.dp
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp),
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .defaultMinSize(
                    minHeight = 1.dp,
                    minWidth = 48.dp
                )
        ) {
            Text(
                text = remember { season.name },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun SeasonPillPreview() {
    FlixclusiveTheme {
        Surface {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(10) { index ->
                    SeasonPill(
                        season = remember {
                            DummyDataForPreview.getTvShow()
                                .seasons.first()
                        },
                        selected = index == 2,
                        onClick = {}
                    )
                }
            }
        }
    }
}
