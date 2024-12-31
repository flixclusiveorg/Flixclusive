package com.flixclusive.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.DBFilm
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.toFilmInstance
import java.util.Date

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String,
    val ownerId: Int,
    val film: DBFilm,
    val addedOn: Date = Date()
)

fun Film.toWatchlistItem(ownerId: Int): WatchlistItem {
    return WatchlistItem(
        id = identifier,
        ownerId = ownerId,
        film = this.toFilmInstance()
    )
}