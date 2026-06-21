package com.flixclusive.feature.mobile.settings.screen.links.util

import com.flixclusive.model.media.MediaMetadata

internal object CacheLinksFormatUtil {
    fun getFormattedTitle(
        media: MediaMetadata,
        season: Int?,
        episode: Int?,
    ): String {
        if (season == null || episode == null) return media.title

        return buildString {
            append(media.title)
            append(": ${getFormattedTitle(season, episode)}")
        }
    }

    fun getFormattedTitle(
        season: Int,
        episode: Int,
    ) = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
}
