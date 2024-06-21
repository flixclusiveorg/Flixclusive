package com.flixclusive.model.tmdb

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.FilmReleaseStatus.COMING_SOON
import com.flixclusive.model.tmdb.FilmReleaseStatus.RELEASED
import com.flixclusive.model.tmdb.FilmReleaseStatus.UNKNOWN
import com.flixclusive.model.tmdb.util.formatDate
import kotlinx.serialization.Serializable

const val DEFAULT_FILM_SOURCE_NAME = "TMDB"

/**
 * Represents the release status of a film.
 *
 * @see RELEASED
 * @see COMING_SOON
 * @see UNKNOWN
 */
enum class FilmReleaseStatus {
    /**
     * The film has been released.
     */
    RELEASED,

    /**
     * The film is coming soon.
     */
    COMING_SOON,

    /**
     * The release status of the film is unknown.
     */
    UNKNOWN
}

/**
 * An abstract representation of a film.
 *
 * @property id The ID of the film. Check out [Film.identifier] as it is more reliable than calling this one.
 * @property filmType The type of film. Could either be [FilmType.MOVIE] or [FilmType.TV_SHOW].
 * @property overview An overview, sypnosis or description of the film.
 * @property adult Indicates whether the film is for adults only.
 * @property runtime The runtime of the film in minutes.
 * @property genres A list of genres associated with the film.
 * @property recommendations A list of recommended films.
 * @property title The title of the film.
 * @property language The language of the film.
 * @property rating The average rating of the film.
 * @property backdropImage The URL to the backdrop image of the film (optional).
 * @property posterImage The URL to the poster image of the film (optional).
 * @property homePage The URL to the home page of the film (optional).
 * @property providerName The name of the provider this film came from.
 * @property imdbId The IMDB ID of the film (optional).
 * @property tmdbId The TMDB ID of the film (optional).
 * @property logoImage The URL to the logo image of the film (optional).
 * @property parsedReleaseDate The parsed release date of the film in a consistent format.
 * @property releaseDate The release date of the film.
 * @property year The year of the film's release (optional).
 * @property releaseStatus The release status of the film. See [FilmReleaseStatus].
 * @property identifier A unique identifier for the film. It could either be [id], [tmdbId], [imdbId], or [title] in the following sequential order.
 * @property year The year of the film's release, extracted from the release date.
 * @property customProperties A map of custom properties associated with the film. Add any properties that your response/resource needs. Also, serialize the value of the property to string.
 *
 * @see FilmDetails
 * @see FilmSearchItem
 */
@Serializable
abstract class Film : java.io.Serializable {
    abstract val id: String?
    /** @see FilmType */
    abstract val filmType: FilmType
    abstract val overview: String?
    abstract val adult: Boolean
    abstract val title: String
    abstract val language: String?
    abstract val rating: Double?
    abstract val backdropImage: String?
    abstract val posterImage: String?
    abstract val homePage: String?
    abstract val releaseDate: String?
    abstract val year: Int?

    open val recommendations: List<FilmSearchItem>
        get() = emptyList()
    open val providerName: String?
        get() = DEFAULT_FILM_SOURCE_NAME
    open val imdbId: String?
        get() = null
    open val tmdbId: Int?
        get() = null
    open val logoImage: String?
        get() = null
    open val parsedReleaseDate: String?
        get() = safeCall { formatDate(releaseDate) } ?: releaseDate
    open val runtime: Int?
        get() = null
    open val genres: List<Genre>
        get() = emptyList()


    /** @see FilmReleaseStatus */
    open val releaseStatus: FilmReleaseStatus
        get() = safeCall {
            when {
                !isDateInFuture(releaseDate) -> RELEASED
                else -> COMING_SOON
            }
        } ?: UNKNOWN

    val identifier: String
        get() = id ?: tmdbId?.toString() ?: imdbId ?: title

    val isFromTmdb: Boolean
        get() = this.tmdbId != null || providerName.equals(DEFAULT_FILM_SOURCE_NAME, ignoreCase = true)

    open val customProperties: Map<String, String?>
        get() = emptyMap()
}


/*
Same properties with data types:
  - backdrop_path
  - original_language
  - overview
  - adult
  - id
  - vote_count
  - vote_average
  - poster_path
  - popularity
* */