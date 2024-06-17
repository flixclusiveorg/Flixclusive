package com.flixclusive.model.tmdb.util

import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.common.tv.Season

const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3/"

fun List<Season>.filterOutUnreleasedSeasons()
    = filterNot {
        it.airDate == null
        && it.episodeCount == 0 && it.rating == 0.0
    }

fun List<Season>.filterOutZeroSeasons()
    = filterNot { it.number == 0 }

fun List<FilmSearchItem>.filterOutUnreleasedFilms()
    = filterNot {
        safeCall { isDateInFuture(it.parsedReleaseDate) } ?: false
            || it.posterImage.isNullOrEmpty()
    }