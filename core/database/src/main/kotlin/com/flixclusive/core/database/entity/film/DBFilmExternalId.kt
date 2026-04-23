package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.flixclusive.model.film.Film
import java.util.Date

@Entity(
    tableName = "film_external_ids",
    primaryKeys = ["filmId", "providerId", "source"],
    foreignKeys = [
        ForeignKey(
            entity = DBFilm::class,
            parentColumns = ["id"],
            childColumns = ["filmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["filmId"]),
        Index(value = ["providerId"]),
    ],
)
data class DBFilmExternalId(
    val filmId: String,
    val providerId: String,
    val source: String,
    val externalId: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) {
    companion object {
        fun Film.toDBFilmExternalIds(): List<DBFilmExternalId> {
            return externalIds.map { (source, externalId) ->
                DBFilmExternalId(
                    filmId = id,
                    providerId = providerId,
                    source = source.name,
                    externalId = externalId,
                )
            }
        }
    }
}
