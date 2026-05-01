package com.flixclusive.feature.mobile.player.util.extensions

import com.flixclusive.core.database.entity.watched.EpisodeProgress

internal fun EpisodeProgress.isSameEpisode(
    otherEpisode: Int,
    otherSeason: Int,
    otherMediaId: String
): Boolean {
    return mediaId == otherMediaId &&
        seasonNumber == otherSeason &&
        episodeNumber == otherEpisode
}
