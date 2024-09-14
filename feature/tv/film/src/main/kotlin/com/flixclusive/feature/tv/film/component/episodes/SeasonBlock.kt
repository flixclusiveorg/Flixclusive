package com.flixclusive.feature.tv.film.component.episodes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SeasonBlock(
    modifier: Modifier = Modifier,
    seasonNumber: Int,
    currentSelectedSeasonNumber: Int,
    onSeasonChange: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isSelected =
        remember(currentSelectedSeasonNumber) { currentSelectedSeasonNumber == seasonNumber }

    val focusedBorder = Border(
        border = BorderStroke(2.dp, Color.White),
        shape = RectangleShape
    )

    Surface(
        modifier = modifier
            .width(200.dp)
            .onFocusChanged {
                scope.launch {
                    if (it.isFocused) {
                        delay(800)
                        onSeasonChange()
                    }
                }
            },
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = focusedBorder
        ),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1F),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) Color.White else LocalContentColor.current.onMediumEmphasis(emphasis = 0.8F),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        onClick = onSeasonChange
    ) {
        Text(
            text = stringResource(LocaleR.string.season_number_formatter, seasonNumber),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center)
        )
    }
}