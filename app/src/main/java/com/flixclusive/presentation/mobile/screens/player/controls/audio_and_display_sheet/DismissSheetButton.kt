package com.flixclusive.presentation.mobile.screens.player.controls.audio_and_display_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DismissSheetButton(
    modifier: Modifier = Modifier,
    onDismissIconClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.White.copy(0.1F), RoundedCornerShape(10))
                .clickable { onDismissIconClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close button for episodes sheet"
            )
        }
    }
}