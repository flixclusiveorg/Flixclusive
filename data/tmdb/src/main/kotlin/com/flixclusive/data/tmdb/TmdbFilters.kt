package com.flixclusive.data.tmdb

import com.flixclusive.core.locale.UiText
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.core.locale.R as LocaleR

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
                    UiText.StringResource(LocaleR.string.all),
                    UiText.StringResource(LocaleR.string.tv_show),
                    UiText.StringResource(LocaleR.string.movie)
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