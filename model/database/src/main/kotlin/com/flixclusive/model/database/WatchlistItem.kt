package com.flixclusive.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmImpl
import com.flixclusive.model.tmdb.toFilmInstance
import java.util.Date

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int = 0,
    val ownerId: Int = 1,
    val addedOn: Date = Date(),
    val film: FilmImpl = FilmImpl()
)

fun Film.toWatchlistItem(): WatchlistItem {
    return WatchlistItem(
        id = id,
        film = this.toFilmInstance()
    )
}