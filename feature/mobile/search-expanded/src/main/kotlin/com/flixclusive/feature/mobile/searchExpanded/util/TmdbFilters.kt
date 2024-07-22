package com.flixclusive.feature.mobile.searchExpanded.util

import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.Filter
import com.flixclusive.core.util.film.FilterGroup
import com.flixclusive.core.util.film.FilterList
import com.flixclusive.core.util.R as UtilR

private const val TMDB_FILTER_LABEL = "Media type"

internal class TmdbFilters(
    options: List<UiText>
) : FilterGroup(
    name = TMDB_FILTER_LABEL,
    Filter.Select(
        options = options,
        state = 0
    ),
) {
    companion object {
        fun getDefaultTmdbFilters()
            = FilterList(
                TmdbFilters(
                    options = listOf(
                        UiText.StringResource(UtilR.string.all),
                        UiText.StringResource(UtilR.string.tv_shows),
                        UiText.StringResource(UtilR.string.movies)
                    )
                ),
            )
    }
}