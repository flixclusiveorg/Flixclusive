package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.BaseSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsDialog(
    modifier: Modifier = Modifier,
    title: String,
    appSettings: AppSettings,
    onDismissRequest: () -> Unit,
    hidePreview: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dialogShape = MaterialTheme.shapes.medium
    val dialogColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            BaseSettingsDialog(
                modifier = modifier,
                title = title,
                onDismissRequest = onDismissRequest,
                content = content
            )

            if(!hidePreview) {
                SubtitlePreview(
                    modifier = Modifier
                        .graphicsLayer {
                            this.shape = dialogShape
                            clip = true
                        }
                        .drawBehind {
                            drawRect(dialogColor)
                        },
                    appSettings = appSettings,
                    shape = dialogShape
                )
            }
        }
    }
}