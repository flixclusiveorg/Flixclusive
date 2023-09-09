package com.flixclusive.presentation.mobile.common.composables

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GradientCircularProgressIndicator(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    ),
    size: Dp = 60.dp
) {
    val gradientColors = remember { listOf(Color.Transparent) + colors }
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
            }
        ),
        label = ""
    )

    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .rotate(angle)
            .border(
                6.dp,
                brush = Brush.sweepGradient(colors = gradientColors),
                shape = CircleShape
            ),
        strokeWidth = 1.dp,
        color = Color.Transparent
    )
}