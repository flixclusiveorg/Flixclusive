package com.flixclusive.core.database.entity.media

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import java.io.Serializable
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
                releaseDate = releaseDate.toReleaseDate(),
            )
        }

        /**
         * Reads a provider's release date, which may arrive in seconds or milliseconds, and returns
         * it as a [Date] — or null when there isn't a usable one.
         *
         * The unit is chosen by which reading lands on a believable date rather than by a fixed
         * cutoff, because the same value has to survive a round trip: [toMediaMetadata] hands back
         * `Date.time`, so whatever this stores is read straight back into here on the next save. A
         * plain "below a trillion means seconds" test fails that for anything released before
         * September 2001 — its milliseconds are below the cutoff too, so each save multiplied it by
         * another thousand.
         *
         * Ambiguity is left only for releases within about twelve weeks of 1 Jan 1970, where both
         * readings are believable; seconds wins there, being overwhelmingly the more common.
         */
        private fun Long?.toReleaseDate(): Date? {
            // 0 is how an absent date round-trips — toMediaMetadata maps a null column to 0L — so
            // honouring it would invent a 1 Jan 1970 release the provider never gave.
            val epoch = this?.takeIf { it != 0L } ?: return null

            val millis = when (epoch) {
                in -MAX_PLAUSIBLE_SECONDS..MAX_PLAUSIBLE_SECONDS -> epoch * 1_000
                in -MAX_PLAUSIBLE_MILLIS..MAX_PLAUSIBLE_MILLIS -> epoch
                // Rows written before this fix hold microseconds; Schema23to24 repairs the ones
                // already stored, and this catches any that come back through a provider.
                in -MAX_PLAUSIBLE_MICROS..MAX_PLAUSIBLE_MICROS -> epoch / 1_000
                else -> return null
            }

            return Date(millis)
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

        /** 1 Jan 2200 in milliseconds — generous for an unreleased title, and the far edge of what
         * any of the three readings is allowed to produce. */
        private const val MAX_PLAUSIBLE_MILLIS = 7_258_118_400_000L
        private const val MAX_PLAUSIBLE_SECONDS = MAX_PLAUSIBLE_MILLIS / 1_000
        private const val MAX_PLAUSIBLE_MICROS = MAX_PLAUSIBLE_MILLIS * 1_000
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
