package com.flixclusive.core.util.film

import com.flixclusive.core.util.R

enum class FilmType(
    val type: String,
    val resId: Int
) {
    MOVIE(
        type = "movie",
        resId = R.string.movies
    ),
    TV_SHOW(
        type = "tv",
        resId = R.string.tv_shows
    );

    companion object {
        fun String?.toFilmType(): FilmType {
            var result = entries.find { it.type == this }
            if(result == null) {
                result = when(this?.lowercase()) {
                    "tv series" -> TV_SHOW
                    "movie" -> MOVIE
                    else -> throw IllegalStateException("Invalid film type: $this")
                }
            }

            return result
        }
    }
}