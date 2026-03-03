package com.flixclusive.feature.mobile.player.component.bottom

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import kotlin.math.hypot

private const val THUMBNAIL_WIDTH_FRACTION = 0.35f
private const val ASPECT_RATIO = 16f / 9f

@Composable
internal fun SeekPreview(
    isVisible: Boolean,
    bitmap: Bitmap?,
    positionText: String,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            reveal.snapTo(0f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SCALE_DURATION,
                    easing = FastOutSlowInEasing,
                ),
            )
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            reveal.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = REVEAL_DURATION,
                    easing = FastOutSlowInEasing,
                ),
            )
            scale.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = SCALE_DURATION,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    if (scale.value > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = scale.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clip(CircularRevealShape(reveal.value)),
        ) {
            SeekPreviewContent(
                bitmap = bitmap,
                positionText = positionText,
            )
        }
    }
}

@Composable
private fun SeekPreviewContent(
    bitmap: Bitmap?,
    positionText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(THUMBNAIL_WIDTH_FRACTION)
                .aspectRatio(ASPECT_RATIO)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ASPECT_RATIO),
                )
            }
        }

        Text(
            text = positionText,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(size = 20.sp),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private class CircularRevealShape(
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val center = Offset(size.width / 2f, size.height)
        val maxRadius = hypot(size.width / 2f, size.height)
        val radius = maxRadius * progress

        val path = Path().apply {
            addOval(Rect(center = center, radius = radius))
        }
        return Outline.Generic(path)
    }
}

private const val SCALE_DURATION = 120
private const val REVEAL_DURATION = 150
