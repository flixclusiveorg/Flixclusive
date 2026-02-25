package com.flixclusive.core.presentation.player.extensions

import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences

private const val PIP_SCALE_FACTOR = 1F
private const val REFERENCE_PIP_WIDTH_DP = 360F

@OptIn(UnstableApi::class)
internal fun SubtitleView.setStyle(
    isInPipMode: Boolean,
    subtitlePrefs: SubtitlesPreferences
) {
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

    if (isInPipMode) {
        val screenWidthDp = context.resources.configuration.screenWidthDp
        val scaledSize = subtitlePrefs.subtitleSize * PIP_SCALE_FACTOR * (screenWidthDp / REFERENCE_PIP_WIDTH_DP)
        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize)
    } else {
        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitlePrefs.subtitleSize)
    }
}
