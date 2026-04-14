package com.flixclusive.feature.mobile.player.util.extensions

import com.flixclusive.core.database.entity.watched.EpisodeProgress

internal fun EpisodeProgress.isSameEpisode(
    otherEpisode: Int,
    otherSeason: Int,
    otherFilmId: String
): Boolean {
    return filmId == otherFilmId &&
        seasonNumber == otherSeason &&
        episodeNumber == otherEpisode
}
