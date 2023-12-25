package com.flixclusive.domain.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmImpl
import com.flixclusive.domain.model.tmdb.toFilmInstance

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int = 0,
    val ownerId: Int = 1,
    val film: FilmImpl = FilmImpl()
)

fun Film.toWatchlistItem(): WatchlistItem {
    return WatchlistItem(
        id = id,
        film = this.toFilmInstance()
    )
}