package com.flixclusive.domain.provider.model

import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.model.film.common.tv.Episode

/**
 * Data class that combines an [Episode] with its corresponding [EpisodeProgress].
 *
 * @property episode The episode details.
 * @property watchProgress The watch progress details for the episode.
 * */
data class EpisodeWithProgress(
    val episode: Episode,
    val watchProgress: EpisodeProgress?
) {
    val number get() = episode.number
    val season get() = episode.season
    val title get() = episode.title
    val overview get() = episode.overview
    val image get() = episode.image
}
