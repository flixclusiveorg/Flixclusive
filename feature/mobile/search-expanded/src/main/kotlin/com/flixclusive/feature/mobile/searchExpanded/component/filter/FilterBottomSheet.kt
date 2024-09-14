package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.provider.filter.FilterGroup
import com.flixclusive.data.tmdb.TmdbFilters.Companion.getDefaultTmdbFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    filters: FilterGroup,
    onUpdateFilters: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp)
        ),
        dragHandle = { DragHandle() }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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

@Composable
private fun DragHandle(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(top = 22.dp, bottom = 5.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2F),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            Modifier.size(
                width = 32.dp,
                height = 4.dp
            )
        )
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