package com.flixclusive.core.util.film.filter

import androidx.compose.runtime.Stable

/**
 * A data class representing a list of filter groups. This provider will be used by the providers.
 *
 * @property list The list of filters groups. Each [FilterGroup] should be extending [Filter] instance.
 */
@Stable
data class FilterList(
    val list: List<FilterGroup>
) : List<FilterGroup> by list {

    /**
     * Secondary constructor that accepts a variable number of filters.
     *
     * @param filters The filters to include in the list.
     */
    constructor(vararg filters: FilterGroup) : this(
        when {
            filters.isNotEmpty() -> filters.asList()
            else -> emptyList()
        }
    )
}