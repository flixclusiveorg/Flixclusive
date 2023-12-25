package com.flixclusive.presentation.common.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.presentation.mobile.common.composables.GradientCircularProgressIndicator
import com.flixclusive.presentation.tv.theme.FlixclusiveTvTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@Composable
fun SourceStateDialog(
    state: VideoDataDialogState,
    isTv: Boolean = false,
    onConsumeDialog: () -> Unit,
) {
    LaunchedEffect(key1 = state) {
        if (state is VideoDataDialogState.Success) {
            onConsumeDialog()
        }
    }

    Dialog(
        onDismissRequest = onConsumeDialog
    ) {
        if (isTv) {
            androidx.tv.material3.Surface(
                tonalElevation = 2.dp,
                shape = androidx.tv.material3.MaterialTheme.shapes.medium,
                content = {
                    SourceStateDialogContent(
                        isTv = true,
                        state = state
                    )
                },
                modifier = Modifier
                    .sizeIn(
                        minHeight = 250.dp,
                        minWidth = 250.dp
                    )
            )
        }
        else {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp),
                shape = androidx.compose.material3.MaterialTheme.shapes.large,
                content = {
                    SourceStateDialogContent(
                        isTv = false,
                        state = state
                    )
                }
            )
        }
    }
}

@Composable
private fun SourceStateDialogContent(
    isTv: Boolean,
    state: VideoDataDialogState
) {
    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = state !is VideoDataDialogState.Error && state !is VideoDataDialogState.Unavailable,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.matchParentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GradientCircularProgressIndicator(
                        colors = if(isTv) {
                            listOf(
                                androidx.tv.material3.MaterialTheme.colorScheme.primary,
                                androidx.tv.material3.MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            listOf(
                                androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    )
                }

                if(isTv) {
                    androidx.tv.material3.Text(
                        text = state.message.asString(),
                        style = androidx.tv.material3.MaterialTheme.typography.labelLarge
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = state.message.asString(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state is VideoDataDialogState.Error || state is VideoDataDialogState.Unavailable,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.matchParentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                if (isTv) {

                    androidx.tv.material3.Icon(
                        painter = painterResource(id = R.drawable.round_error_outline_24),
                        contentDescription = "Error icon",
                        tint = androidx.tv.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 15.dp)
                    )

                    androidx.tv.material3.Text(
                        text = state.message.asString(),
                        style = androidx.tv.material3.MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.round_error_outline_24),
                        contentDescription = "Error icon",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 15.dp)
                    )

                    androidx.compose.material3.Text(
                        text = state.message.asString(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(
    device = Devices.TV_1080p,
    showSystemUi = true,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    fontScale = 1.0f,
)
@Composable
fun VideoPlayerDialogPreview() {
    val delay = 2500L
    val state: VideoDataDialogState by flow {
        emit(VideoDataDialogState.Fetching())
        delay(delay)
        emit(VideoDataDialogState.Extracting())
        delay(delay)
        emit(VideoDataDialogState.Error())
        delay(delay)
        emit(VideoDataDialogState.Unavailable())
        delay(delay)
        emit(VideoDataDialogState.Success)
        delay(delay)
        emit(VideoDataDialogState.Idle)
    }.collectAsStateWithLifecycle(initialValue = VideoDataDialogState.Idle)

    FlixclusiveTvTheme {
        SourceStateDialog(
            state = state,
            true
        ) {

        }
    }
}