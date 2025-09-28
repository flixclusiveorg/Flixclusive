package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.presentation.mobile.components.material3.dialog.TextAlertDialog
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveGridCellsCount
import com.flixclusive.feature.mobile.searchExpanded.ViewLabelHeader
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchSearchHistoryView(
    searchHistory: List<SearchHistory>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    deleteSearchHistoryItem: (SearchHistory) -> Unit,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var searchHistoryToDelete by rememberSaveable { mutableStateOf<SearchHistory?>(null) }

    LazyVerticalGrid(
        modifier = modifier,
        contentPadding = scaffoldPadding,
        columns = getAdaptiveGridCellsCount(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ViewLabelHeader(label = stringResource(id = LocaleR.string.search_history))
        }

        items(
            items = searchHistory,
            key = { it.id },
        ) { item ->
            SearchHistoryBlock(
                modifier = Modifier.animateItem(),
                item = item,
                onClick = {
                    onQueryChange(item.query)
                    onSearch()
                },
                onLongClick = {
                    searchHistoryToDelete = item
                },
                onArrowClick = {
                    onQueryChange(item.query)
                },
            )
        }
    }

    if (searchHistoryToDelete != null) {
        TextAlertDialog(
            title = stringResource(id = LocaleR.string.delete_search_history_item),
            message = stringResource(id = LocaleR.string.delete_search_history_item_message),
            onConfirm = { deleteSearchHistoryItem(searchHistoryToDelete!!) },
            onDismiss = { searchHistoryToDelete = null },
        )
    }
}
