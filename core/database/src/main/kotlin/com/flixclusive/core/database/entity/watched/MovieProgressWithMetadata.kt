package com.flixclusive.core.database.entity.watched

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.film.DBFilm

/**
 * Represents a movie watch progress with associated metadata.
 *
 * @see MovieProgress
 * @see WatchProgressWithMetadata
 * @see DBFilm
 * */
data class MovieProgressWithMetadata(
    @Embedded override val watchData: MovieProgress,
    @Relation(
        parentColumn = "filmId",
        entityColumn = "id",
    )
    override val film: DBFilm,
) : WatchProgressWithMetadata
