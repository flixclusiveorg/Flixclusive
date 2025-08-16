package com.flixclusive.core.database.entity.film

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import java.io.Serializable
import java.util.Date

/**
 *
 * Represents a film entity in the database.
 *
 * This class extends the [Film] model and implements [Serializable] for easy serialization.
 * It is annotated with [Entity] to define it as a Room database entity.
 * */
@Entity(tableName = "films")
data class DBFilm(
    @PrimaryKey
    override val id: String,
    override val providerId: String = "",
    override val imdbId: String? = null,
    override val tmdbId: Int? = null,
    override val language: String? = null,
    override val adult: Boolean = false,
    override val title: String = "",
    override val runtime: Int? = null,
    override val backdropImage: String? = null,
    override val posterImage: String? = null,
    override val overview: String? = null,
    override val homePage: String? = null,
    override val releaseDate: String? = null,
    override val logoImage: String? = null,
    override val year: Int? = null,
    override val filmType: FilmType = FilmType.MOVIE,
    override val rating: Double? = null,
    override val customProperties: Map<String, String?> = emptyMap(),
    val hasRecommendations: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
) : Film(),
    Serializable {
    companion object {
        internal const val DB_FILM_VALID_RECOMMENDATIONS_COUNT = 3

        /**
         * Converts a [Film] to a [DBFilm].
         * */
        fun Film.toDBFilm(): DBFilm {
            return DBFilm(
                id = identifier,
                imdbId = imdbId,
                tmdbId = tmdbId,
                adult = adult,
                title = title,
                posterImage = posterImage,
                backdropImage = backdropImage,
                logoImage = logoImage,
                overview = overview,
                filmType = filmType,
                rating = rating,
                language = language,
                releaseDate = releaseDate,
                year = year,
                runtime = runtime,
                homePage = homePage,
                providerId = providerId,
                customProperties = customProperties,
                hasRecommendations = recommendations.size >= DB_FILM_VALID_RECOMMENDATIONS_COUNT,
            )
        }
    }
}
