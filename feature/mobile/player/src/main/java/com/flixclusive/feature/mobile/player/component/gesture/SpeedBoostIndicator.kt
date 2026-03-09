package com.flixclusive.feature.mobile.player.component.gesture

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.GlassSurface

@Composable
internal fun SpeedBoostIndicator(
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        shape = MaterialTheme.shapes.small,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    ) {
        Text(
            text = "2x",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
