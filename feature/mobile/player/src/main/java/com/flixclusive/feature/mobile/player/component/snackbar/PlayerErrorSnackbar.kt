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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.GlassSurface
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.SnackbarError
import kotlinx.coroutines.delay

private val ErrorAccentColor = Color(0xFFEF5350)
private const val EXIT_ANIMATION_MS = 300L

@Composable
internal fun PlayerErrorSnackbar(
    snackbarState: PlayerSnackbarState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        items(
            snackbarState.errors,
            key = { it.key },
        ) { error ->
            ErrorSnackbarItem(
                error = error,
                onDismiss = { snackbarState.dismissError(error.key) },
            )
        }
    }
}

@Composable
private fun ErrorSnackbarItem(
    error: SnackbarError,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(error.key) {
        delay(PlayerSnackbarState.ERROR_AUTO_DISMISS_MS)
        isVisible = false
        delay(EXIT_ANIMATION_MS)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessLow,
            ),
        ) { -it } + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            animationSpec = tween(EXIT_ANIMATION_MS.toInt()),
        ) { -it / 2 } + fadeOut(animationSpec = tween(200)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.4f),
        ) {
            GlassSurface(
                shape = MaterialTheme.shapes.small,
                accentColor = ErrorAccentColor,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = error.text,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}
