package com.flixclusive.presentation.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlayerSnackbarMessageType {
    Quality,
    Subtitle,
    Server,
    Episode
}

data class PlayerSnackbarMessage(
    val message: String,
    val type: PlayerSnackbarMessageType,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

@Composable
fun PlayerSnackbarVisuals(
    messageData: PlayerSnackbarMessage,
    onDismissMessage: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }
    val message = messageData.message
    val duration = messageData.duration
    val type = messageData.type

    val snackbarShape = RoundedCornerShape(15)

    val hideSnackbar = {
        scope.launch {
            isVisible = false
            delay(200)
            onDismissMessage()
        }
    }

    LaunchedEffect(messageData) {
        delay(200)
        isVisible = true

        val durationInLong = when (duration) {
            SnackbarDuration.Short -> 4000L
            SnackbarDuration.Long -> 10000L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
        }

        delay(durationInLong)
        hideSnackbar()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally { -1000 } + fadeIn(),
        exit = fadeOut() + slideOutHorizontally { -1000 },
    ) {
        Box(
            modifier = Modifier
                .padding(
                    bottom = 15.dp,
                    start = 15.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = 250.dp, max = 450.dp)
                    .height(50.dp)
                    .graphicsLayer {
                        shape = snackbarShape
                        clip = true
                    }
                    .background(Color.Black.copy(0.50F))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(0.5F),
                        shape = snackbarShape
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                )

                if(type != PlayerSnackbarMessageType.Episode) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                            .clickable {
                                hideSnackbar()
                            }
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}