package com.flixclusive.domain.utils

import com.flixclusive.data.dto.consumet.ConsumetEpisode
import com.flixclusive.domain.model.consumet.Subtitle
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.tmdb.TMDBEpisode

object ConsumetUtils {
    fun getConsumetEpisodeId(
        episode: TMDBEpisode,
        listOfEpisode: List<ConsumetEpisode>
    ): String? {
        val episodeId = listOfEpisode
            .find {
                it.title.contains(episode.title, ignoreCase = true)
                || it.number == episode.episode && episode.season == it.season
            }?.id

        return episodeId
    }

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

    fun VideoData.getAutoSourceUrl(): String {
        return sources.let { sources ->
            sources.find { it.quality.contains("auto", ignoreCase = true) } ?: sources[0]
        }.url
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