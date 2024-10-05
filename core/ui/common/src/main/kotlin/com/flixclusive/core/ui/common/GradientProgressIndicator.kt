package com.flixclusive.core.ui.common

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme

@Composable
fun GradientCircularProgressIndicator(
    modifier: Modifier = Modifier,
    colors: List<Color>,
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

// https://medium.com/@kappdev/creating-a-smooth-animated-progress-bar-in-jetpack-compose-canvas-drawing-and-gradient-animation-ddf07f77bb56
@Composable
fun GradientLinearProgressIndicator(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    glowRadius: Dp? = 4.dp,
) {
    // Create an infinite animation transition
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val strokeWidth = 4.dp
    val strokeCap = StrokeCap.Round
    val gradientAnimationSpeed = 2500
    val progressAnimSpec: AnimationSpec<Float> = tween(
        durationMillis = 720,
        easing = LinearOutSlowInEasing
    )

    // Animates offset value transition from 0 to 1
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        label = "",
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = gradientAnimationSpeed,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    // Creates a brush that updates based on the animated offset
    val brush = remember(offset) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val step = 1f / colors.size  // Calculate step size
                val start = step / 2 // Define start position

                // Calculate original positions for each color
                val originalSpots = List(colors.size) { start + (step * it) }

                // Apply animation offset to each color position
                val transformedSpots = originalSpots.map { spot ->
                    val shiftedSpot = (spot + offset)
                    // Wrap around if exceeds 1
                    if (shiftedSpot > 1f) shiftedSpot - 1f else shiftedSpot
                }

                // Combine colors with their transformed positions
                val pairs = colors.zip(transformedSpots).sortedBy { it.second }

                // Margin for gradient outside the progress bar
                val margin = size.width / 2

                // Create the linear gradient shader with colors and positions
                return LinearGradientShader(
                    colors = pairs.map { it.first },
                    colorStops = pairs.map { it.second },
                    from = Offset(-margin, 0f),
                    to = Offset(size.width + margin, 0f)
                )
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        label = "",
        targetValue = 1F,
        animationSpec = progressAnimSpec
    )

    Canvas(modifier) {
        val width = this.size.width
        val height = this.size.height

        // Create a Paint object
        val paint = Paint().apply {
            // Enable anti-aliasing for smoother lines
            isAntiAlias = true
            style = PaintingStyle.Stroke
            this.strokeWidth = strokeWidth.toPx()
            this.strokeCap = strokeCap
            // Apply the animated gradient shader
            shader = brush.createShader(size)
        }

        // Handle optional glow effect
        glowRadius?.let { radius ->
            paint.asFrameworkPaint().apply {
                setShadowLayer(radius.toPx(), 0f, 0f, android.graphics.Color.WHITE)
            }
        }

        // Draw the progress line if progress is greater than 0
        if (animatedProgress > 0f) {
            drawIntoCanvas { canvas ->
                canvas.drawLine(
                    p1 = Offset(0f, height / 2f),
                    p2 = Offset(width * animatedProgress, height / 2f),
                    paint = paint
                )
            }
        }
    }
}

@Preview
@Composable
private fun GradientProgressIndicatorPreview() {
    FlixclusiveTheme {
        Surface {
            GradientLinearProgressIndicator(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}