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

    val progress get() = watchProgress?.progress
    val duration get() = watchProgress?.duration

    override fun equals(other: Any?): Boolean {
        if (other is Episode) {
            return this.episode == other
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = episode.hashCode()
        result = 31 * result + (watchProgress?.hashCode() ?: 0)
        result = 31 * result + number
        result = 31 * result + season
        result = 31 * result + title.hashCode()
        result = 31 * result + overview.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        return result
    }
}
