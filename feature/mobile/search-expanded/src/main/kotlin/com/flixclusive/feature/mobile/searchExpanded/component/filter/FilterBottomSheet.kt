package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.material3.CommonBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.data.tmdb.util.TMDBFilters.Companion.getDefaultTMDBFilters
import com.flixclusive.provider.filter.FilterGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    filters: FilterGroup,
    onUpdateFilters: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    CommonBottomSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(filters) {
                FilterItem(
                    filter = it,
                    filterGroup = filters,
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
                filters = getDefaultTMDBFilters().first(),
                onUpdateFilters = { },
                onDismissRequest = { },
            )
        }
    }
}
