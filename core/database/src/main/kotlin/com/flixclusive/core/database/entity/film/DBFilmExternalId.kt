package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// TODO: Move to `core-stubs`
object ExternalMetadataSource {
    const val TMDB = "tmdb"
    const val IMDB = "imdb"
    const val TVDB = "tvdb"
    const val TRAKT = "trakt"
    const val ANILIST = "anilist"
}

@Entity(
    tableName = "film_external_ids",
    foreignKeys = [
        ForeignKey(
            entity = DBFilm::class,
            parentColumns = ["id"],
            childColumns = ["filmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["filmId", "providerId", "source"], unique = true),
        Index(value = ["filmId"]),
        Index(value = ["providerId"]),
    ],
)
data class DBFilmExternalId(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filmId: String,
    val providerId: String,
    val source: String,
    val externalId: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)
