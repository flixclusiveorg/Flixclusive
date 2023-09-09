package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsDialog(
    modifier: Modifier = Modifier,
    title: String,
    appSettings: AppSettings,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val dialogShape = MaterialTheme.shapes.medium
    val buttonMinHeight = 50.dp
    val dialogColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 220.dp)
                    .graphicsLayer {
                        shape = dialogShape
                        clip = true
                    }
                    .drawBehind {
                        drawRect(dialogColor)
                    }
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier
                            .padding(10.dp)
                    )

                    content()

                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White)
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(
                            text = stringResource(R.string.close_label),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }

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