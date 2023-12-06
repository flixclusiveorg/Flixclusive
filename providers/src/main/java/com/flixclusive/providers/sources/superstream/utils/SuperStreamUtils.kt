package com.flixclusive.providers.sources.superstream.utils

import com.flixclusive.providers.models.common.MediaType

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

        fun toMediaType(): MediaType {
            return if (this == Series) MediaType.TvShow else MediaType.Movie
        }

        companion object {
            fun getSSMediaType(value: Int?): SSMediaType {
                return values().firstOrNull { it.value == value } ?: Movies
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