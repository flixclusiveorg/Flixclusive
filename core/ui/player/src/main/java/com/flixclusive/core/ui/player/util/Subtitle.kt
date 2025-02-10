package com.flixclusive.core.ui.player.util

import android.content.Context
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import com.flixclusive.core.locale.R as LocaleR

internal fun Subtitle.isOffSubtitle(context: Context): Boolean {
    val offSubtitleLabel = context.getString(LocaleR.string.off_subtitles)
    return offSubtitleLabel == language && type == SubtitleSource.EMBEDDED && url.isEmpty()
}
