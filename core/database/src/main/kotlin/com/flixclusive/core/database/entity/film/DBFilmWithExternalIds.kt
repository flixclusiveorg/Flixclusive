package com.flixclusive.core.database.entity.film

import androidx.room.Embedded
import androidx.room.Relation

data class DBFilmWithExternalIds(
    @Embedded val film: DBFilm,
    @Relation(
        entity = DBFilmExternalId::class,
        parentColumn = "id",
        entityColumn = "filmId",
    )
    val externalIds: List<DBFilmExternalId>,
) {
    val id get() = film.id
    val title get() = film.title
    val posterImage get() = film.posterImage
    val filmType get() = film.filmType
    val providerId get() = film.providerId
}
