package com.flixclusive.feature.mobile.search.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.provider.filter.Filter
import com.flixclusive.provider.filter.FilterGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    filters: () -> FilterGroup,
    onUpdateFilters: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    CommonBottomSheet(onDismissRequest = onDismissRequest) {
        val updatedFilters by rememberUpdatedState(filters)

        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(updatedFilters()) {
                FilterItem(
                    filter = it,
                    filterGroup = updatedFilters,
                    onUpdateFilters = onUpdateFilters,
                )
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
                filters = {
                    FilterGroup(
                        name = "Type",
                        list = listOf(
                            Filter.Select(
                                name = "Type",
                                options = listOf(
                                    "All",
                                    "Movie",
                                    "TV Show",
                                ),
                                state = 0,
                            ),
                        )
                    )
                },
                onUpdateFilters = { },
                onDismissRequest = { },
            )
        }
    }
}
