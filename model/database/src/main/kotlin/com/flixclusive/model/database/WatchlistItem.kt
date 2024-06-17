package com.flixclusive.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.tmdb.DBFilm
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.toFilmInstance
import java.util.Date

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String = "",
    val ownerId: Int = 1,
    val addedOn: Date = Date(),
    val film: DBFilm = DBFilm()
)

fun Film.toWatchlistItem(): WatchlistItem {
    return WatchlistItem(
        id = identifier,
        film = this.toFilmInstance()
    )
}