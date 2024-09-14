package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.formatPlayButtonLabel
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.film.FilmReleaseStatus
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun FilmScreenButtons(
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean,
    releaseStatus: FilmReleaseStatus,
    watchHistoryItem: WatchHistoryItem?,
    onPlayClick: () -> Unit,
    onWatchlistClick: () -> Unit,
) {
    val playButtonLabel = remember(watchHistoryItem) {
        formatPlayButtonLabel(watchHistoryItem)
    }
    val (watchlistIcon, watchlistContentDescription) = remember(isInWatchlist) {
        val icon = if(isInWatchlist)
            R.drawable.added_bookmark to LocaleR.string.added_to_watchlist_button
        else R.drawable.add_bookmark to LocaleR.string.add_to_watchlist_button

        icon
    }

    val transition = rememberInfiniteTransition(label = "")
    val translateAnimation = transition.animateFloat(
        initialValue = -50F,
        targetValue = 300F,
        animationSpec = infiniteRepeatable(
            animation = tween(3000), repeatMode = RepeatMode.Reverse
        ),
        label = ""
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
            if(releaseStatus != FilmReleaseStatus.COMING_SOON) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .widthIn(min = 105.dp)
                        .graphicsLayer {
                            shape = CircleShape
                            shadowElevation = 8.dp.toPx()
                            clip = true
                        }
                        .drawBehind {
                            drawRect(buttonGradient)
                        },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.play),
                            contentDescription = stringResource(LocaleR.string.play_button)
                        )

                        Text(
                            text = playButtonLabel.asString(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            else {
                OutlinedButton(
                    onClick = {},
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.White.onMediumEmphasis(0.08F)
                    ),
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = Color.White.onMediumEmphasis(0.3F)
                    ),
                    enabled = false
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.time_circle_outlined),
                            contentDescription = stringResource(LocaleR.string.coming_soon)
                        )

                        Text(
                            text = stringResource(LocaleR.string.coming_soon),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onWatchlistClick,
            modifier = Modifier.size(50.dp)
        ) {
            Icon(
                painter = painterResource(watchlistIcon),
                contentDescription = stringResource(watchlistContentDescription),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ComingSoonButton() {
    FlixclusiveTheme {
        Surface {
            OutlinedButton(
                onClick = {},
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.White.onMediumEmphasis(0.08F)),
                border = BorderStroke(0.5.dp, Color.White.onMediumEmphasis(0.3F)),
                enabled = false
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.added_bookmark),
                        contentDescription = stringResource(LocaleR.string.more_like_this)
                    )

                    Text(
                        text = stringResource(LocaleR.string.more_like_this),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}