package com.flixclusive.core.util.film

import com.flixclusive.core.util.R
import com.flixclusive.core.util.film.FilmType.MOVIE
import com.flixclusive.core.util.film.FilmType.TV_SHOW

/**
 * Represents the type of a film.
 *
 * @property type The string representation of the film type.
 * @property resId The resource ID associated with the film type.
 *
 * @see MOVIE
 * @see TV_SHOW
 */
enum class FilmType(
    val type: String,
    val resId: Int
) {
    /**
     * Represents a movie.
     */
    MOVIE(
        type = "movie",
        resId = R.string.movies
    ),

    /**
     * Represents a TV show.
     */
    TV_SHOW(
        type = "tv",
        resId = R.string.tv_shows
    );

    /**
     * Companion object for [FilmType].
     */
    companion object {
        /**
         * Converts a string to a [FilmType].
         *
         * @param this The string to convert.
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