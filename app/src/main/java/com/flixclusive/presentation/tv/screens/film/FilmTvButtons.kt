package com.flixclusive.presentation.tv.screens.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.createInitialFocusRestorerModifiers
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.drawAnimatedBorder
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.ifElse
import com.flixclusive.presentation.utils.FormatterUtils

@Composable
fun FilmTvButtons(
    watchHistoryItem: WatchHistoryItem?,
    isTvShow: Boolean,
    shouldFocusOnPlayButton: Boolean,
    shouldFocusOnEpisodesButton: MutableState<Boolean>,
    onPlay: () -> Unit,
    onWatchlistClick: () -> Unit,
    onSeeMoreEpisodes: () -> Unit = {},
) {
    val buttonShape: Shape = MaterialTheme.shapes.extraSmall
    val focusRestorerModifiers = createInitialFocusRestorerModifiers()

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = focusRestorerModifiers.parentModifier
    ) {
        PlayButton(
            watchHistoryItem = watchHistoryItem,
            shape = buttonShape,
            onClick = onPlay,
            modifier = focusRestorerModifiers.childModifier
                .ifElse(
                    condition = shouldFocusOnPlayButton,
                    ifTrueModifier = Modifier.focusOnInitialVisibility(remember { mutableStateOf(false) })
                )
        )

        if (isTvShow) {
            EpisodesAndMoreButton(
                shape = buttonShape,
                onClick = onSeeMoreEpisodes,
                modifier = Modifier.focusOnInitialVisibility(isVisible = shouldFocusOnEpisodesButton)
            )
        }

        WatchlistButton(
            shape = buttonShape,
            onClick = onWatchlistClick
        )
    }
}

@Composable
private fun PlayButton(
    modifier: Modifier = Modifier,
    watchHistoryItem: WatchHistoryItem?,
    shape: Shape,
    onClick: () -> Unit,
) {
    var isButtonFocused by remember { mutableStateOf(false) }
    val playButtonLabel = remember(watchHistoryItem) {
        FormatterUtils.formatPlayButtonLabel(watchHistoryItem)
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
            .ifElse(
                condition = !isButtonFocused,
                ifTrueModifier = Modifier.drawAnimatedBorder(
                    strokeWidth = 2.dp,
                    shape = shape,
                    brush = Brush.sweepGradient(animatedGradientColors),
                    durationMillis = 15000
                ),
                ifFalseModifier = Modifier.clip(shape)
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
                painter = painterResource(id = R.drawable.play),
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

@Composable
private fun EpisodesAndMoreButton(
    modifier: Modifier = Modifier,
    shape: Shape,
    onClick: () -> Unit,
) {
    val buttonBorder = Border(
        border = BorderStroke(
            width = 2.dp,
            color = colorOnMediumEmphasisTv(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                emphasis = 0.4F
            )
        ),
        shape = shape
    )

    OutlinedButton(
        onClick = onClick,
        border = OutlinedButtonDefaults.border(
            border = buttonBorder,
            focusedBorder = buttonBorder,
            pressedBorder = buttonBorder
        ),
        shape = OutlinedButtonDefaults.shape(shape),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_video_library_24),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = stringResource(id = R.string.episodes),
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

@Composable
private fun WatchlistButton(
    shape: Shape,
    onClick: () -> Unit,
) {
    var isButtonFocused by remember { mutableStateOf(false) }
    val buttonBorder = Border(
        border = BorderStroke(
            width = 2.dp,
            color = colorOnMediumEmphasisTv(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                emphasis = 0.4F
            )
        ),
        shape = shape
    )

    OutlinedButton(
        onClick = onClick,
        shape = OutlinedButtonDefaults.shape(shape),
        border = OutlinedButtonDefaults.border(
            border = buttonBorder,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        modifier = Modifier
            .clip(shape)
            .animateContentSize()
            .onFocusChanged {
                isButtonFocused = it.isFocused
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.round_add_24),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
            )

            AnimatedVisibility(
                visible = isButtonFocused,
                enter = fadeIn() + slideInHorizontally(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Text(
                    text = stringResource(id = R.string.add_to_watchlist),
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
}