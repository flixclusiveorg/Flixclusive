package com.flixclusive.feature.mobile.provider.add.filter.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters

@Composable
internal fun AddProviderFilterBottomSheet(
    filters: () -> List<AddProviderFilterType<*>>,
    onUpdateFilter: (Int, AddProviderFilterType<*>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape =
            MaterialTheme.shapes.small.copy(
                bottomEnd = CornerSize(0.dp),
                bottomStart = CornerSize(0.dp),
            ),
        dragHandle = { DragHandle() },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(
                items = filters(),
                key = { _, filter -> filter.title.asString(context) },
            ) { i, filter ->
                Filter(
                    filter = filter,
                    onUpdateFilter = { onUpdateFilter(i, it) },
                )
            }
        }
    }
}

@Composable
private fun Filter(
    filter: AddProviderFilterType<*>,
    onUpdateFilter: (AddProviderFilterType<*>) -> Unit,
) {
    when (filter) {
        is AddProviderFilterType.MultiSelect -> {
            MultiSelectFilter(
                filter = filter,
                onUpdateFilter = {
                    val newFilter = when (filter) {
                        is AuthorsFilters -> filter.copy(selectedValue = it)
                        is RepositoriesFilters -> filter.copy(selectedValue = it)
                        is LanguagesFilters -> filter.copy(selectedValue = it)
                        is ProviderTypeFilters -> filter.copy(selectedValue = it)
                        is StatusFilters -> filter.copy(selectedValue = it)
                        else -> throw IllegalArgumentException("Illegal multi select filter: $filter")
                    }

                    onUpdateFilter(newFilter)
                },
            )
        }

        is AddProviderFilterType.Sort<*> -> {
            SortFilter(
                filter = filter,
                onUpdateFilter = {
                    if (filter is CommonSortFilters) {
                        onUpdateFilter(filter.copy(selectedValue = it))
                    }
                },
            )
        }
    }
}
