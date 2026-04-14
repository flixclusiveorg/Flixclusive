package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.flixclusive.model.film.Film
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
        // TODO: Film will be refactored to have a list of external ids,
        //  so this mapping will need to be updated accordingly.
        fun Film.toDBFilmExternalIds(): List<DBFilmExternalId> {
            val externalIds = mutableListOf<DBFilmExternalId>()

            if (imdbId != null) {
                externalIds.add(
                    DBFilmExternalId(
                        filmId = identifier,
                        providerId = providerId,
                        source = ExternalMetadataSource.IMDB,
                        externalId = imdbId!!,
                    )
                )
            }

            if (tmdbId != null) {
                externalIds.add(
                    DBFilmExternalId(
                        filmId = identifier,
                        providerId = providerId,
                        source = ExternalMetadataSource.TMDB,
                        externalId = tmdbId.toString(),
                    )
                )
            }

            return externalIds.toList()
        }
    }
}
