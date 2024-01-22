package com.flixclusive.feature.mobile.player.controls.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.flixclusive.core.ui.common.util.noIndicationClickable

@Composable
internal fun BasePlayerDialog(
    onDismissSheet: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
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
                .noIndicationClickable {
                    onDismissSheet()
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.8F)
                .clip(dialogShape)
                .background(dialogColor)
                .noIndicationClickable { },
            content = content
        )
    }
}