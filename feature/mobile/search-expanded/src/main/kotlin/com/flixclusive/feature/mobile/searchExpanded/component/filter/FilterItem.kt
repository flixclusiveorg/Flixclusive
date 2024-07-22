package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.runtime.Composable
import com.flixclusive.core.util.film.Filter

@Composable
internal fun FilterItem(filter: Filter<*>) {
    when (filter) {
        is Filter.CheckBox -> {
            TODO()
        }
        is Filter.Select<*> -> {

        }
        is Filter.Sort -> {
            TODO()
        }
        is Filter.TriState -> {
            TODO()
        }
    }
}