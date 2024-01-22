package com.flixclusive.data.tmdb.util

import com.flixclusive.core.network.retrofit.dto.tv.TvShowSeasonsPreview
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.TMDBSearchItem

internal fun List<TvShowSeasonsPreview>.filterOutUnreleasedSeasons()
    = filterNot { it.airDate == null && it.episodeCount == 0 && it.voteAverage == 0.0 }

internal fun List<TvShowSeasonsPreview>.filterOutZeroSeasons()
    = filterNot { it.seasonNumber == 0 }

internal fun List<TMDBSearchItem>.filterOutUnreleasedFilms()
    = filterNot { isDateInFuture(it.dateReleased) || it.posterImage.isNullOrEmpty() }