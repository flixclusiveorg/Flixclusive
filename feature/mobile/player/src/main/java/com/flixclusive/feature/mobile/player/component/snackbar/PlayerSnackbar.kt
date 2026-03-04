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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.flixclusive.core.presentation.player.ui.state.SnackbarMessage
import kotlinx.coroutines.delay

private const val EXIT_ANIMATION_MS = 250L

private val SnackbarEnterTransition =
    slideInHorizontally(
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow,
        ),
    ) { it } + fadeIn(animationSpec = tween(300))

private val SnackbarExitTransition =
    slideOutHorizontally(
        animationSpec = tween(EXIT_ANIMATION_MS.toInt()),
    ) { it / 2 } + fadeOut(animationSpec = tween(200))

@Composable
internal fun PlayerSnackbar(
    snackbarState: PlayerSnackbarState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier,
    ) {
        items(
            snackbarState.messages,
            key = { it.key },
        ) { message ->
            MessageSnackbarItem(
                message = message,
                onDismiss = { snackbarState.dismissMessage(message.key) },
            )
        }

        item(key = "countdown") {
            CountdownSnackbarItem(
                countdown = snackbarState.countdown,
                countdownKey = snackbarState.countdownKey,
            )
        }
    }
}

@Composable
private fun CountdownSnackbarItem(
    countdown: SnackbarCountdown?,
    countdownKey: Long,
) {
    var isVisible by remember { mutableStateOf(false) }
    var displayedCountdown by remember { mutableStateOf<SnackbarCountdown?>(null) }

    val text by remember {
        derivedStateOf {
            displayedCountdown?.let { it.format(it.valueProvider()) } ?: ""
        }
    }

    LaunchedEffect(countdownKey) {
        if (countdown != null) {
            if (isVisible) {
                isVisible = false
                delay(EXIT_ANIMATION_MS)
            }
            displayedCountdown = countdown
            isVisible = true
        } else if (displayedCountdown != null) {
            isVisible = false
            delay(EXIT_ANIMATION_MS)
            displayedCountdown = null
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = SnackbarEnterTransition,
        exit = SnackbarExitTransition,
    ) {
        SnackbarSurface(text = text)
    }
}

@Composable
private fun MessageSnackbarItem(
    message: SnackbarMessage,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(message.key) {
        delay(message.durationMs)
        isVisible = false
        delay(EXIT_ANIMATION_MS)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = SnackbarEnterTransition,
        exit = SnackbarExitTransition,
    ) {
        SnackbarSurface(text = message.text)
    }
}

@Composable
private fun SnackbarSurface(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.4f),
        horizontalArrangement = Arrangement.End,
    ) {
        GlassSurface(
            shape = MaterialTheme.shapes.small,
            accentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
