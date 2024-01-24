@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component.buttons

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.util.drawAnimatedBorder
import com.flixclusive.core.ui.common.util.formatPlayButtonLabel
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun PlayButton(
    modifier: Modifier = Modifier,
    watchHistoryItem: WatchHistoryItem?,
    shape: Shape,
    onClick: () -> Unit,
) {
    var isButtonFocused by remember { mutableStateOf(false) }
    val playButtonLabel = remember(watchHistoryItem) {
        formatPlayButtonLabel(watchHistoryItem)
    }

    val buttonSizeAsFloatState by animateFloatAsState(
        targetValue = if(isButtonFocused) 1.1F else 1F,
        label = ""
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnimation = transition.animateFloat(
        initialValue = -50F,
        targetValue = 300F,
        animationSpec = infiniteRepeatable(
            animation = tween(3000), repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )
    val animatedGradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )
    val buttonGradient = remember {
        Brush.horizontalGradient(
            colors = gradientColors,
            startX = translateAnimation.value,
            endX = translateAnimation.value + 500F
        )
    }

    OutlinedButton(
        onClick = onClick,
        border = OutlinedButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        shape = OutlinedButtonDefaults.shape(shape),
        colors = OutlinedButtonDefaults.colors(
            focusedContainerColor = Color.Transparent
        ),
        modifier = modifier
            .scale(buttonSizeAsFloatState)
            .ifElse(
                condition = !isButtonFocused,
                ifTrueModifier = Modifier.drawAnimatedBorder(
                    strokeWidth = 2.dp,
                    shape = shape,
                    brush = Brush.sweepGradient(animatedGradientColors),
                    durationMillis = 15000
                ),
                ifFalseModifier = Modifier
                    .clip(shape)
            )
            .drawBehind {
                if (isButtonFocused) {
                    drawRect(buttonGradient)
                } else drawRect(Color.Transparent)
            }
            .onFocusChanged {
                isButtonFocused = it.isFocused
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.play),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
            )

            Text(
                text = playButtonLabel.asString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun PlayButtonPreview() {

}