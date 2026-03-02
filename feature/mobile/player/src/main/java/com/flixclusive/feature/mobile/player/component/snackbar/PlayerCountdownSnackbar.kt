package com.flixclusive.feature.mobile.player.component.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.GlassSurface
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.SnackbarCountdown
import kotlinx.coroutines.delay

private const val EXIT_ANIMATION_MS = 250L

@Composable
internal fun PlayerCountdownSnackbar(
    snackbarState: PlayerSnackbarState,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    var displayedCountdown by remember { mutableStateOf<SnackbarCountdown?>(null) }

    LaunchedEffect(snackbarState.countdownKey) {
        val current = snackbarState.countdown
        if (current != null) {
            if (isVisible) {
                isVisible = false
                delay(EXIT_ANIMATION_MS)
            }
            displayedCountdown = current
            isVisible = true
        } else {
            if (isVisible) {
                isVisible = false
                delay(EXIT_ANIMATION_MS)
                displayedCountdown = null
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessLow,
            ),
        ) { it } + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            animationSpec = tween(EXIT_ANIMATION_MS.toInt()),
        ) { it / 2 } + fadeOut(animationSpec = tween(200)),
        modifier = modifier,
    ) {
        displayedCountdown?.let { countdown ->
            val text by remember(countdown) {
                derivedStateOf {
                    countdown.format(countdown.valueProvider())
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.4f),
                horizontalArrangement = Arrangement.End,
            ) {
                GlassSurface(
                    shape = MaterialTheme.shapes.small,
                    accentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = text,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
