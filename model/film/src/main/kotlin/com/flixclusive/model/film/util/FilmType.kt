package com.flixclusive.model.film.util

import com.flixclusive.model.film.util.FilmType.MOVIE
import com.flixclusive.model.film.util.FilmType.TV_SHOW

/**
 * Represents the type of a film.
 *
 * @property type The string representation of the film type.
 * @property stringId The resource ID associated with the film type.
 *
 * @see MOVIE
 * @see TV_SHOW
 */
enum class FilmType(
    val type: String
) {
    /**
     * Represents a movie.
     */
    MOVIE(type = "movie"),

    /**
     * Represents a TV show.
     */
    TV_SHOW(type = "tv");

    /**
     * Companion object for [FilmType].
     */
    companion object {
        /**
         * Converts a string to a [FilmType].
         *
         * @return The corresponding [FilmType], or throws an [IllegalStateException] if the string is invalid.
         */
        fun String?.toFilmType(): FilmType {
            var result = entries.find { it.type == this }
            if (result == null) {
                result = when (this?.lowercase()) {
                    "tv series", "tv", "show", "tv show", "tvshow" -> TV_SHOW
                    "movie" -> MOVIE
                    else -> throw IllegalStateException("Invalid film type: $this")
                }
            }

            return result
        }
    }
}