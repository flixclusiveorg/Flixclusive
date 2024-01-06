package com.flixclusive.provider.superstream.util

import com.flixclusive.core.util.film.FilmType

internal object SuperStreamUtils {
    // Random 32 length string
    fun randomToken(): String {
        return (0..31).joinToString("") {
            (('0'..'9') + ('a'..'f')).random().toString()
        }
    }

    enum class SSMediaType(val value: Int) {
        Series(2),
        Movies(1);

        fun toFilmType() = if (this == Series) FilmType.TV_SHOW else FilmType.MOVIE
        companion object {
            fun getSSMediaType(value: Int?): SSMediaType {
                return entries.firstOrNull { it.value == value } ?: Movies
            }
        }
    }

    fun getExpiryDate(): Long {
        // Current time + 12 hours
        return (System.currentTimeMillis() / 1000) + 60 * 60 * 12
    }

    fun String.isError(lazyMessage: String) {
        if(
            contains(
                other = "error",
                ignoreCase = true
            )
        ) throw Exception(lazyMessage)
    }
}