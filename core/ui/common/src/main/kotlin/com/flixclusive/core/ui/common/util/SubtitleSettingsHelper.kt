package com.flixclusive.core.ui.common.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.model.datastore.player.CaptionStylePreference

@Composable
fun CaptionStylePreference.getTextStyle(): TextStyle {
    return MaterialTheme.typography.labelLarge.run {
        when(this@getTextStyle) {
            CaptionStylePreference.Normal -> copy(fontWeight = FontWeight.Normal)
            CaptionStylePreference.Bold -> copy(fontWeight = FontWeight.Bold)
            CaptionStylePreference.Italic -> copy(fontStyle = FontStyle.Italic)
            CaptionStylePreference.Monospace -> copy(fontFamily = FontFamily.Monospace)
        }
    }
}