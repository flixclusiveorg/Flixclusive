package com.flixclusive.presentation.common.player.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.flixclusive.providers.models.common.Subtitle
import java.util.Locale

object SubtitleUtils {
    fun getSubtitleMimeType(source: String, subtitle: Subtitle): String? {
        val isLocalSubtitle = subtitle.url.contains("content://")
        val uri = if(isLocalSubtitle) {
            subtitle.lang
        } else subtitle.url

        return when {
            source.contains(".mp4") && uri.endsWith(".vtt", true) -> MimeTypes.APPLICATION_MP4VTT
            uri.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
            uri.endsWith(".ssa", true) -> MimeTypes.TEXT_SSA
            uri.endsWith(".ttml", true) || uri.endsWith(".xml", true) -> MimeTypes.APPLICATION_TTML
            uri.endsWith(".srt", true) -> MimeTypes.APPLICATION_SUBRIP
            else -> null
        }
    }

    fun List<MediaItem.SubtitleConfiguration>.getIndexFromLanguage(lang: String? = null): Int {
        val index = if(lang == null) {
            indexOfFirst {
                it.language?.contains("en", ignoreCase = true) == true
            }
        } else {
            indexOfFirst {
                it.language?.contains(lang, ignoreCase = true) == true
                || it.language?.contains(Locale(lang).displayLanguage, true) == true
            }
        }

        return when(index) {
            -1 -> {
                if(lang != null)
                    getIndexFromLanguage()
                else 0
            }
            else -> index
        }
    }
}