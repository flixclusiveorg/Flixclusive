package com.flixclusive.data.tmdb.util

import com.flixclusive.core.network.retrofit.dto.tv.TvShowSeasonsPreview
import com.flixclusive.core.util.film.isDateInFuture
import com.flixclusive.model.tmdb.TMDBSearchItem


internal fun filterOutZeroSeasons(seasons: List<TvShowSeasonsPreview>): List<TvShowSeasonsPreview> {
    return seasons.filter { it.seasonNumber != 0 }
}

internal fun filterOutUnreleasedRecommendations(recommendations: List<TMDBSearchItem>): List<TMDBSearchItem> {
    return recommendations
        .filterNot { isDateInFuture(it.dateReleased) || it.posterImage.isNullOrEmpty() }
}