package com.flixclusive.core.database.entity.watchlist

import androidx.room.Embedded
import androidx.room.Relation
import com.flixclusive.core.database.entity.film.DBFilm

data class WatchlistWithMetadata(
    @Embedded val watchlist: Watchlist,
    @Relation(
        parentColumn = "filmId",
        entityColumn = "id",
    )
    val film: DBFilm,
) {
    val id get() = watchlist.id
    val filmId get() = watchlist.filmId
}
