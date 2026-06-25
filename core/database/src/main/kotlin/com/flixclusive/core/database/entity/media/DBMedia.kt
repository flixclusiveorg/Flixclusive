package com.flixclusive.core.database.entity.media

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import java.io.Serializable
import java.time.Instant
import java.util.Date

/**
 * Represents a media entity in the database.
 *
 * Stores only stable identity fields. Mutable metadata like ratings, overviews,
 * and backdrops are intentionally excluded.
 * */
@Entity(tableName = "media")
data class DBMedia(
    @PrimaryKey
    val id: String,
    val title: String,
    val providerId: String,
    val adult: Boolean,
    val type: MediaType,
    val overview: String?,
    val posterImage: String?,
    val language: String?,
    val rating: Double?,
    val backdropImage: String?,
    val releaseDate: Date?,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable {
    companion object {
        fun MediaMetadata.toDBMedia(): DBMedia {
            val msDate = if (releaseDate != null && releaseDate!! < 1000000000000L) {
                releaseDate!! * 1000
            } else {
                releaseDate
            }

            return DBMedia(
                id = id,
                adult = adult,
                overview = overview,
                providerId = providerId,
                type = type,
                title = title,
                posterImage = posterImage,
                language = language,
                rating = rating,
                backdropImage = backdropImage,
                releaseDate = Date.from(Instant.ofEpochSecond(msDate ?: 0L)),
            )
        }

        fun DBMedia.toMediaMetadata(externalIds: Map<MediaIdSource, String>): PartialMedia {
            return PartialMedia(
                id = id,
                adult = adult,
                overview = overview,
                providerId = providerId,
                type = type,
                title = title,
                posterImage = posterImage,
                language = language,
                rating = rating,
                backdropImage = backdropImage,
                releaseDate = releaseDate?.time ?: 0L,
                externalIds = externalIds,
            )
        }
    }
}

@Deprecated("Only used for migration from v2.1.3, should be removed after migration is complete")
internal data class DBFilmV213(
    @PrimaryKey
    val id: String,
    val providerId: String = "",
    @Deprecated(
        "Use sourceIds[MediaIdSource.IMDB] instead.",
        replaceWith = ReplaceWith("sourceIds[MediaIdSource.IMDB]")
    )
    val imdbId: String? = null,
    @Deprecated(
        "Use sourceIds[MediaIdSource.TMDB]?.toIntOrNull() instead.",
        replaceWith = ReplaceWith("sourceIds[MediaIdSource.TMDB]?.toIntOrNull()")
    )
    val tmdbId: Int? = null,
    val language: String? = null,
    val adult: Boolean = false,
    val title: String = "",
    val runtime: Int? = null,
    val backdropImage: String? = null,
    val posterImage: String? = null,
    val overview: String? = null,
    val homePage: String? = null,
    val releaseDate: Date? = null,
    val logoImage: String? = null,
    val year: Int? = null,
    val filmType: String = "MOVIE",
    val rating: Double? = null,
    val customProperties: Map<String, String?> = emptyMap(),
    val hasRecommendations: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Serializable
