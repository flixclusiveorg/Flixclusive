package com.flixclusive.feature.mobile.player.component.subtitle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.dropShadow
import com.flixclusive.core.presentation.player.model.CueWithTiming

@Composable
internal fun SubtitleCueItem(
    cue: CueWithTiming,
    isActive: () -> Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val textColor by animateColorAsState(
        targetValue = when {
            isActive() -> Color.White
            isPressed -> Color.White.copy(alpha = 0.65f)
            else -> Color.White.copy(alpha = 0.4f)
        },
        animationSpec = tween(if (isPressed) 100 else 300),
        label = "cue_color"
    )

    val scale = animateFloatAsState(
        targetValue = when {
            isActive() && isPressed -> 0.96f
            isActive() -> 1f
            isPressed -> 0.82f
            else -> 0.85f
        },
        animationSpec = tween(if (isPressed) 100 else 300),
        label = "cue_scale"
    )

    val baseStyle = MaterialTheme.typography.titleMedium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
        ) {
            Text(
                text = remember { "${cue.startTimeMs.formatTimestamp()} – ${cue.endTimeMs.formatTimestamp()}" },
                color = textColor.copy(alpha = textColor.alpha * 0.5f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Text(
                text = remember { cue.cue.joinToString("\n") },
                color = textColor,
                style = if (isActive()) {
                    baseStyle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    ).dropShadow()
                } else {
                    baseStyle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                },
            )
        }
    }
}

private fun Long.formatTimestamp(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
