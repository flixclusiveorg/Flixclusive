package com.flixclusive.feature.mobile.library.details.util

import com.flixclusive.model.film.Film
import java.util.Locale

internal object FilmUtils {
    fun Film.matches(query: String): Boolean {
        if (title.contains(query, true)) {
            return true
        }

        overview?.let { overview ->
            if (overview.contains(query, true)) {
                return true
            }
        }

        genres.forEach { genre ->
            if (genre.name.contains(query, true)) {
                return true
            }
        }

        language?.let { language ->
            if (language.contains(query, true)) {
                return true
            }

            val displayLanguage = Locale
                .Builder()
                .setLanguage(language)
                .build()
                .displayLanguage

            if (displayLanguage.contains(query, true)) {
                return true
            }
        }

        if (providerId.contains(query, true)) {
            return true
        }

        year?.let { year ->
            if (year.toString().contains(query, true)) {
                return true
            }
        }

        return false
    }
}
