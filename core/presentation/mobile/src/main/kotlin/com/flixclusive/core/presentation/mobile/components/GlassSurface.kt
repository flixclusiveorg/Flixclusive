package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .dropShadow(shape = shape) {
                radius = 40f
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.9f),
                    ),
                )
            }
            .border(
                width = 0.5.dp,
                shape = shape,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.2f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.8f),
                        Color.Black.copy(alpha = 0.6f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            )
            .innerShadow(shape = shape) {
                radius = 40f
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(200f, 200f),
                )
            },
        content = content,
    )
}
