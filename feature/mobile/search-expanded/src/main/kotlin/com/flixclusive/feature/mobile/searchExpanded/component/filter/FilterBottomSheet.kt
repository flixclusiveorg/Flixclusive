package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.util.film.Filter
import com.flixclusive.core.util.film.FilterGroup
import com.flixclusive.feature.mobile.searchExpanded.component.filter.component.SelectDropdownMenu
import com.flixclusive.feature.mobile.searchExpanded.util.TmdbFilters.Companion.getDefaultTmdbFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    filters: FilterGroup,
    onUpdateFilters: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp)
        ) {
            items(filters) {
                when (it) {
                    is Filter.CheckBox -> TODO()
                    is Filter.Select<*> -> {
                        SelectDropdownMenu(
                            label = filters.name,
                            options = it.options,
                            selected = it.state,
                            onSelect = { option ->
                                it.state = option
                                onUpdateFilters()
                            }
                        )
                    }
                    is Filter.Sort -> TODO()
                    is Filter.TriState -> TODO()
                }
            }
        }
    }
}

@Preview
@Composable
private fun FilterBottomSheetPreview() {
    FlixclusiveTheme {
        Surface {
            FilterBottomSheet(
                filters = getDefaultTmdbFilters().first(),
                onUpdateFilters = { },
                onDismissRequest = { },
            )
        }
    }
}