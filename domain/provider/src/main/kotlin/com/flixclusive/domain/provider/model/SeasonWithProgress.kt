package com.flixclusive.domain.provider.model

import com.flixclusive.model.media.common.tv.Season

/**
 * A data class representing a season along with its watch progress for each episode.
 * */
data class SeasonWithProgress(
    val season: Season.Full,
    val episodes: List<EpisodeWithProgress>,
) {
    val number get() = season.number
    val title get() = season.title
    val overview get() = season.overview
}
