package com.flixclusive.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.Film
import java.util.Date

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey override val id: String,
    override val film: DBFilm,
    val ownerId: Int,
    val addedOn: Date = Date(),
) : DBFilmItem

fun Film.toWatchlistItem(ownerId: Int): WatchlistItem {
    return WatchlistItem(
        id = identifier,
        ownerId = ownerId,
        film = this.toDBFilm(),
    )
}
