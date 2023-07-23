package com.flixclusive.domain.utils

import com.flixclusive.data.dto.tmdb.tv.TvShowSeasonsPreview
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.presentation.common.Functions.isDateInFuture

object TMDBUtils {
    fun filterOutZeroSeasons(seasons: List<TvShowSeasonsPreview>): List<TvShowSeasonsPreview> {
        return seasons.filter { it.seasonNumber != 0 }
    }

    fun filterOutUnreleasedRecommendations(recommendations: List<TMDBSearchItem>): List<TMDBSearchItem> {
        return recommendations
            .filterNot { isDateInFuture(it.dateReleased) || it.posterImage.isNullOrEmpty() }
    }

}