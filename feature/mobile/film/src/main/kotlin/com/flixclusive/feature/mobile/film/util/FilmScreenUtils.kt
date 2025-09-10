package com.flixclusive.feature.mobile.film.util

import com.flixclusive.feature.mobile.film.ContentTabType
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.util.FilmType

internal object FilmScreenUtils {
    fun getTabs(film: Film): List<ContentTabType> {
        val tabs = mutableListOf<ContentTabType>()

        if (film is TvShow || film.filmType == FilmType.TV_SHOW) {
            tabs.add(ContentTabType.Episodes)
        }

        if (film.recommendations.isNotEmpty()) {
            tabs.add(ContentTabType.MoreLikeThis)
        }

        if (film is Movie && film.collection?.films?.isNotEmpty() == true) {
            tabs.add(ContentTabType.Collections)
        }

        return tabs.toList()
    }
}
