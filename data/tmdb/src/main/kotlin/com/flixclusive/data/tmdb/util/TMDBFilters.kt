package com.flixclusive.data.tmdb.util

import com.flixclusive.core.common.locale.UiText
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.core.strings.R as LocaleR

class TMDBFilters(
    options: List<UiText>,
) : FilterGroup(
    name = TMDB_FILTER_LABEL,
    Filter.Select(
        name = TMDB_FILTER_LABEL,
        options = options,
        state = FILTER_ALL,
    ),
) {
    companion object {
        private const val TMDB_FILTER_LABEL = "Media type"
        internal const val FILTER_ALL = 0
        internal const val FILTER_TV_SHOW = 1
        internal const val FILTER_MOVIES = 2

        fun getDefaultTMDBFilters() =
            FilterList(
                TMDBFilters(
                    options = listOf(
                        UiText.from(LocaleR.string.all),
                        UiText.from(LocaleR.string.tv_show),
                        UiText.from(LocaleR.string.movie),
                    ),
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
