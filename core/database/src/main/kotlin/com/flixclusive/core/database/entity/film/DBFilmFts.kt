package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.Fts3
import com.flixclusive.model.film.Film

@Fts3
@Entity(tableName = "films_fts")
data class DBFilmFts(
    val filmId: String,
    val title: String,
    val overview: String,
) {
    companion object {
        fun Film.toDBFilmFts() = DBFilmFts(
            filmId = identifier,
            overview = overview ?: "",
            title = title,
        )
    }
}
