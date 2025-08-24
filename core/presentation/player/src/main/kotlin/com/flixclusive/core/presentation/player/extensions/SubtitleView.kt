package com.flixclusive.core.presentation.player.extensions

import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences

private const val FONT_SIZE_PIP_MODE = 8F // Equivalent to 8dp

@OptIn(UnstableApi::class)
internal fun SubtitleView.setStyle(
    subtitlePrefs: SubtitlesPreferences
) {
    // Modify subtitle style
    val style = CaptionStyleCompat(
        subtitlePrefs.subtitleColor,
        subtitlePrefs.subtitleBackgroundColor,
        Color.TRANSPARENT,
        subtitlePrefs.subtitleEdgeType.type,
        Color.BLACK,
        subtitlePrefs.subtitleFontStyle.typeface,
    )

    setApplyEmbeddedFontSizes(false)
    setApplyEmbeddedStyles(false)
    setStyle(style)
    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitlePrefs.subtitleSize)
}

@OptIn(UnstableApi::class)
internal fun SubtitleView.setStyleInPipMode() {
    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_PIP_MODE)
}
