package com.flixclusive.core.ui.common.util

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.model.datastore.user.player.CaptionStylePreference

fun CaptionStylePreference.getTextStyle(): TextStyle {
    return when(this@getTextStyle) {
        CaptionStylePreference.Normal -> TextStyle(fontWeight = FontWeight.Normal)
        CaptionStylePreference.Bold -> TextStyle(fontWeight = FontWeight.Bold)
        CaptionStylePreference.Italic -> TextStyle(fontStyle = FontStyle.Italic)
        CaptionStylePreference.Monospace -> TextStyle(fontFamily = FontFamily.Monospace)
    }
}
