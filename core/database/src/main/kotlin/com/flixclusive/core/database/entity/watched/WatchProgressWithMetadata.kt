package com.flixclusive.core.database.entity.watched

import com.flixclusive.core.database.entity.film.DBFilm

/**
 * Represents a watch progress item with associated metadata.
 * */
sealed interface WatchProgressWithMetadata {
    val watchData: WatchProgress
    val film: DBFilm

    val id get() = watchData.id
    val filmId get() = film.id
}
