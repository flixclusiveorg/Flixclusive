package com.flixclusive.feature.tv.player.controls.settings.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ConfirmButton(
    modifier: Modifier = Modifier,
    label: String,
    isEmphasis: Boolean,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = when(isEmphasis) {
        true -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surface.onMediumEmphasis(0.8F)
        false -> Color(0xFF5F5F5F) to Color(0xFFFFFFFF).onMediumEmphasis()
    }

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = containerColor,
            focusedContentColor = contentColor
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.extraSmall
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = MaterialTheme.colorScheme.primary,
                elevation = 15.dp
            ),
            pressedGlow = Glow(
                elevationColor = MaterialTheme.colorScheme.primary,
                elevation = 40.dp
            ),
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraSmall),
        modifier = modifier
            .heightIn(min = 20.dp)
            .width(150.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(5.dp)
        )
    }
}