package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.dialog.TextAlertDialog
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenViewModel
import com.flixclusive.model.database.SearchHistory
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchSearchHistoryView(
    modifier: Modifier = Modifier,
    viewModel: SearchExpandedScreenViewModel,
) {
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    var searchHistoryToDelete by rememberSaveable { mutableStateOf<SearchHistory?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        stickyHeader {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 5.dp)
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.search_history),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = LocalContentColor.current.onMediumEmphasis(0.8F)
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        items(
            items = searchHistory,
            key = { it.id }
        ) { item ->
            SearchHistoryBlock(
                modifier = Modifier.animateItem(),
                item = item,
                onClick = {
                    viewModel.onQueryChange(item.query)
                    viewModel.onSearch()
                },
                onLongClick = {
                    searchHistoryToDelete = item
                },
                onArrowClick = {
                    viewModel.onQueryChange(item.query)
                }
            )
        }
    }

    if (searchHistoryToDelete != null) {
        TextAlertDialog(
            label = stringResource(id = LocaleR.string.delete_search_history_item),
            description = stringResource(id = LocaleR.string.delete_search_history_item_message),
            onConfirm = { viewModel.deleteSearchHistoryItem(searchHistoryToDelete!!) },
            onDismiss = { searchHistoryToDelete = null }
        )
    }
}