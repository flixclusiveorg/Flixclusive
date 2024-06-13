package com.flixclusive.core.ui.player.util

import android.content.Context
import androidx.media3.common.MimeTypes
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import java.util.Locale
import kotlin.math.max
import com.flixclusive.core.util.R as UtilR

internal fun getSubtitleMimeType(subtitle: Subtitle): String? {
    val isLocalSubtitle = subtitle.url.contains("content://")
    val uri = if(isLocalSubtitle) {
        subtitle.language
    } else subtitle.url

    return when {
        uri.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
        uri.endsWith(".ssa", true) -> MimeTypes.TEXT_SSA
        uri.endsWith(".ttml", true) || uri.endsWith(".xml", true) -> MimeTypes.APPLICATION_TTML
        uri.endsWith(".srt", true) -> MimeTypes.APPLICATION_SUBRIP
        else -> null
    }
}

internal fun List<Subtitle>.getPreferredSubtitleIndex(langCode: String): Int {
    val locale = Locale(langCode)

    val index = indexOfFirst {
        val preferredLocale = Locale(it.language)

        it.language.equals(langCode, ignoreCase = true)
        || preferredLocale.displayLanguage.contains(locale.displayLanguage, true)
    }

    return max(index, 0)
}

/**
 * Initializes the subtitles by adding an "Off" option and updating the video data with the new subtitles.
 */
internal fun List<Subtitle>.addOffSubtitle(
    context: Context
): List<Subtitle> {
    return listOf(
        Subtitle(
            url = "",
            language = context.getString(UtilR.string.off_subtitles),
            type = SubtitleSource.EMBEDDED
        )
    ) + this
}