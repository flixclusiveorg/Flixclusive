package com.flixclusive.feature.mobile.player.component.bottom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import java.util.Locale


private val Float.toPlayerSpeed: String
    get() = String.format(Locale.ROOT, "%.2fx", this)


@Composable
internal fun ResizeModeSheet(
    currentResizeMode: ResizeMode,
    onResizeModeChange: (ResizeMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val bgColor = MaterialTheme.colorScheme.primary
    val modes by remember {
        derivedStateOf {
            ResizeMode.entries
        }
    }

    BackHandler {
        onDismiss()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .dropShadow(shape = shape) {
                radius = 40f
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            }
            .border(
                width = 0.5.dp,
                shape = shape,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.2f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .fillMaxAdaptiveWidth(
                compact = 0.4f,
                medium = 0.6f,
                expanded = 0.65f
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        bgColor.copy(alpha = 0.1f),
                        Color.Black.copy(alpha = 0.8f),
                        Color.Black.copy(alpha = 0.6f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = shape
            )
            .innerShadow(shape = shape) {
                radius = 40f
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(200f, 200f)
                )
            }
            .padding(10.dp)
    ) {
        // First 4 modes have fill the width,
        // the last 3 should fill remaining space
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 0..3) {
                val mode = modes.getOrNull(i) ?: continue

                TextButton(
                    onClick = { onResizeModeChange(mode) },
                    shape = shape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                        .weight(1f),
                    border = if (mode == currentResizeMode)
                        ButtonDefaults.outlinedButtonBorder().copy(
                            width = 0.5.dp
                        ) else null,
                ) {
                    Text(
                        text = stringResource(mode.getStringId()),
                        style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 4..<modes.size) {
                val mode = modes.getOrNull(i) ?: continue

                TextButton(
                    onClick = { onResizeModeChange(mode) },
                    shape = shape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                        .weight(1f),
                    border = if (mode == currentResizeMode)
                        ButtonDefaults.outlinedButtonBorder().copy(
                            width = 0.5.dp
                        ) else null,
                ) {
                    Text(
                        text = stringResource(mode.getStringId()),
                        style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                    )
                }
            }
        }
    }
}
