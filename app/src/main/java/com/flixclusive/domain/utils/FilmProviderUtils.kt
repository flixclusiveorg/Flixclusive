package com.flixclusive.domain.utils

import com.flixclusive.providers.models.common.Subtitle

object FilmProviderUtils {

    /**
     * Initializes the subtitles by adding an "Off" option and updating the video data with the new subtitles.
     * If the list of subtitles available contains the default language (English), it initializes the next that specific subtitle.
     */
    fun List<Subtitle>.addOffSubtitles(): List<Subtitle> {
        return listOf(
            Subtitle(
                url = "",
                lang = "Off"
            )
        ) + this
    }
}