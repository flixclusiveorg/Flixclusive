package com.flixclusive.presentation.mobile.screens.player.controls.video_settings_dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive_provider.models.common.VideoDataServer
import com.flixclusive.presentation.common.PlayerUiState

@Composable
fun VideoSettingsDialog(
    state: PlayerUiState,
    servers: List<VideoDataServer>,
    onPlaybackSpeedChange: (Int) -> Unit,
    onVideoServerChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val dialogColor = Color.Black.copy(0.6F)
    val dialogShape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissSheet()
                },
            contentAlignment = Alignment.Center
        ) {}

        Box(
            modifier = Modifier
                .fillMaxSize(0.8F)
                .clip(dialogShape)
                .background(dialogColor)
        ) {
            Row {
                PlaybackSpeedPanel(
                    modifier = Modifier
                        .weight(1F)
                        .padding(15.dp),
                    selectedPlaybackSpeed = state.selectedPlaybackSpeedIndex,
                    onPlaybackSpeedChange = onPlaybackSpeedChange
                )

                VideoServersPanel(
                    modifier = Modifier
                        .weight(1F)
                        .padding(15.dp),
                    servers = servers,
                    selectedServer = state.selectedServer,
                    onVideoServerChange = onVideoServerChange
                )
            }

            ConfirmButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                onClick = onDismissSheet
            )
        }
    }
}

@Composable
private fun ConfirmButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(5.dp)
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(0.1F),
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .heightIn(min = 50.dp)
        ) {
            Text(
                text = stringResource(R.string.close_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Light
            )
        }
    }
}

