package com.flixclusive.domain.utils

import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.common.VideoData
import java.util.Locale

object FilmProviderUtils {

    /**
     * Initializes the subtitles by adding an "Off" option and updating the video data with the new subtitles.
     * If the list of subtitles available contains the default language (English), it initializes the next that specific subtitle.
     */
    fun VideoData.initializeSubtitles(): VideoData {
        val newSubtitles = listOf(
            Subtitle(
                url = "",
                lang = "Off"
            )
        ) + subtitles

        return copy(subtitles = newSubtitles)
    }

    fun VideoData.getSubtitleIndex(lang: String? = null): Int {
        val index = if(lang == null) {
            subtitles.indexOfFirst {
                 it.lang.contains("en", ignoreCase = true)
            }
        } else {
            subtitles.indexOfFirst {
                it.lang.contains(lang, ignoreCase = true)
                || it.lang.contains(Locale(lang).displayLanguage, true)
            }
        }

        return when(index) {
            -1 -> {
                if(lang != null)
                    getSubtitleIndex()
                else 0
            }
            else -> index
        }
    }
}