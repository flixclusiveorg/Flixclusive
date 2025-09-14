package com.flixclusive.domain.provider.model

import com.flixclusive.model.film.common.tv.Season

/**
 * A data class representing a season along with its watch progress for each episode.
 * */
data class SeasonWithProgress(
    val season: Season,
    val episodes: List<EpisodeWithProgress>,
) {
    val number get() = season.number
    val title get() = season.name
    val overview get() = season.overview
}
