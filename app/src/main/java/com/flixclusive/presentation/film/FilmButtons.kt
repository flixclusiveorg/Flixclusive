package com.flixclusive.presentation.film

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.presentation.common.Formatter.formatPlayButtonLabel
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.UiText

@Composable
fun FilmButtons(
    modifier: Modifier = Modifier,
    isInWatchlistProvider: () -> Boolean,
    watchHistoryItem: WatchHistoryItem?,
    onPlayClick: () -> Unit,
    onWatchlistClick: () -> Unit,
) {
    val playButtonLabel = remember(watchHistoryItem) {
        formatPlayButtonLabel(watchHistoryItem)
    }
    val watchlistIcon = remember(isInWatchlistProvider()) {
        val icon = if(isInWatchlistProvider())
            R.drawable.added_bookmark
        else R.drawable.add_bookmark

        IconResource.fromDrawableResource(icon)
    }
    val watchlistContentDescription = remember(isInWatchlistProvider()) {
        val icon = if(isInWatchlistProvider())
            R.string.added_to_watchlist_button
        else R.string.add_to_watchlist_button

        UiText.StringResource(icon)
    }

    val transition = rememberInfiniteTransition()
    val translateAnimation = transition.animateFloat(
        initialValue = -50F,
        targetValue = 300F,
        animationSpec = infiniteRepeatable(
            animation = tween(3000), repeatMode = RepeatMode.Reverse
        )
    )
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        ),
        startX = translateAnimation.value,
        endX = translateAnimation.value + 500F
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .weight(1F)
        ) {
            Button(
                onClick = onPlayClick,
                modifier = Modifier.widthIn(min = 105.dp)
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(100)
                        clip = true
                    }
                    .drawBehind {
                        drawRect(buttonGradient)
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.play)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.play_button).asString()
                    )

                    Text(
                        text = playButtonLabel.asString(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        IconButton(
            onClick = onWatchlistClick,
            modifier = Modifier.size(50.dp)
        ) {
            Icon(
                painter = watchlistIcon.asPainterResource(),
                contentDescription = watchlistContentDescription.asString(),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}