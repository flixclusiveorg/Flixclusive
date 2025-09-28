package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveGridCellsCount
import com.flixclusive.feature.mobile.searchExpanded.ViewLabelHeader
import com.flixclusive.feature.mobile.searchExpanded.util.Constant
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.max
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchProvidersView(
    providerMetadataList: ImmutableList<ProviderMetadata>,
    selectedProviderId: String,
    scaffoldPadding: PaddingValues,
    onChangeProvider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = remember {
        providerMetadataList.indexOfFirst { it.id == selectedProviderId }
    }

    val listState = rememberLazyGridState(initialFirstVisibleItemIndex = max(selectedIndex, 0))

    LazyVerticalGrid(
        modifier = modifier,
        state = listState,
        contentPadding = scaffoldPadding,
        columns = getAdaptiveGridCellsCount(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ViewLabelHeader(label = stringResource(id = LocaleR.string.get_search_results_from))
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            val tmdbMetadata = Constant.tmdbProviderMetadata

            SearchProviderBlock(
                providerMetadata = tmdbMetadata,
                isSelected = selectedProviderId == tmdbMetadata.id,
                onClick = { onChangeProvider(tmdbMetadata.id) },
            )
        }

        items(
            providerMetadataList,
            key = { item -> item.id },
        ) { item ->
            SearchProviderBlock(
                providerMetadata = item,
                isSelected = item.id == selectedProviderId,
                onClick = { onChangeProvider(item.id) },
            )
        }
    }
}
