package com.flixclusive.domain.utils

import com.flixclusive_provider.models.common.Subtitle
import com.flixclusive_provider.models.common.VideoData

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

    fun VideoData.getDefaultSubtitleIndex(): Int {
        return when(
            val index = subtitles.indexOfFirst {
                it.lang.contains("eng", ignoreCase = true)
            }
        ) {
            -1 -> 0
            else -> index
        }
    }
}