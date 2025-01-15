package com.flixclusive.core.ui.mobile.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun PlainTooltipBox(
    description: String,
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    state: TooltipState = rememberTooltipState(isPersistent = true),
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        enableUserInput = enableUserInput,
        positionProvider = positionProvider,
        state = state,
        focusable = focusable,
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 1.dp,
                shadowElevation = 10.dp,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(text = description)
            }
        },
        content = content,
    )
}
