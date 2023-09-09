package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player.VideoPlayerSettingsDialog
import com.flixclusive_provider.models.common.MediaServer

@Composable
fun VideoPlayerServer(
    appSettings: AppSettings,
    onChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(appSettings.preferredServer) }

    VideoPlayerSettingsDialog(
        title = stringResource(id = R.string.server),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            MediaServer.values().forEach {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (it.serverName == selectedOption),
                            onClick = {
                                onOptionSelected(it.serverName)
                                onChange(it.serverName)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (it.serverName == selectedOption),
                        onClick = {
                            onOptionSelected(it.serverName)
                            onChange(it.serverName)
                        }
                    )

                    Text(
                        text = it.serverName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}