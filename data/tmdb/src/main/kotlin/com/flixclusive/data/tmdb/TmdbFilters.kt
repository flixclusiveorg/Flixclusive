package com.flixclusive.data.tmdb

import com.flixclusive.core.util.R
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.filter.Filter
import com.flixclusive.core.util.film.filter.FilterGroup
import com.flixclusive.core.util.film.filter.FilterList

private const val TMDB_FILTER_LABEL = "Media type"
internal const val FILTER_ALL = 0
internal const val FILTER_TV_SHOW = 1
internal const val FILTER_MOVIES = 2

class TmdbFilters(
    options: List<UiText>
) : FilterGroup(
    name = TMDB_FILTER_LABEL,
    Filter.Select(
        name = TMDB_FILTER_LABEL,
        options = options,
        state = FILTER_ALL
    ),
) {
    companion object {
        fun getDefaultTmdbFilters()
            = FilterList(
            TmdbFilters(
                options = listOf(
                    UiText.StringResource(R.string.all),
                    UiText.StringResource(R.string.tv_show),
                    UiText.StringResource(R.string.movie)
                )
            ),
        )

        fun getMediaTypeFromInt(selected: Int): String {
            return when (selected) {
                FILTER_TV_SHOW -> "tv"
                FILTER_MOVIES -> "movie"
                else -> "multi"
            }
        }
    }
}