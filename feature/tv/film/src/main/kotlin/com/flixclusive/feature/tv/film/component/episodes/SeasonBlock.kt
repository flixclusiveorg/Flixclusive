package com.flixclusive.feature.tv.film.component.episodes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SeasonBlock(
    modifier: Modifier = Modifier,
    seasonNumber: Int,
    currentSelectedSeasonNumber: Int,
    onSeasonChange: () -> Unit,
) {
    val isSelected =
        remember(currentSelectedSeasonNumber) { currentSelectedSeasonNumber == seasonNumber }
    var isFocused by remember { mutableStateOf(false) }

    val style = if (isSelected) {
        MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val focusedBorder = Border(
        border = BorderStroke(2.dp, Color.White),
        shape = RectangleShape
    )

    Surface(
        modifier = modifier
            .width(200.dp)
            .onFocusChanged {
                isFocused = it.isFocused

                if (isFocused) {
                    onSeasonChange()
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
            text = "Season $seasonNumber",
            style = style,
            modifier = Modifier
                .padding(16.dp)
        )
    }
}