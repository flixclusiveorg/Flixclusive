package com.flixclusive.core.database.entity.watched

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.film.DBFilm

/**
 * Represents a progress item in the watch history for an episode,
 *
 * @see WatchProgressWithMetadata
 * @see EpisodeProgress
 * @see DBFilm
 * */
data class EpisodeProgressWithMetadata(
    @Embedded override val watchData: EpisodeProgress,
    @Relation(
        parentColumn = "filmId",
        entityColumn = "id",
    )
    override val film: DBFilm,
) : WatchProgressWithMetadata
