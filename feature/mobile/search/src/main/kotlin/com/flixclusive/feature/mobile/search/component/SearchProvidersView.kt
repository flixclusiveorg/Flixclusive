package com.flixclusive.feature.mobile.search.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveGridCellsCount
import com.flixclusive.feature.mobile.search.R
import com.flixclusive.feature.mobile.search.SearchProvider
import com.flixclusive.feature.mobile.search.ViewLabelHeader
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchProvidersView(
    providers: Async<List<SearchProvider>>,
    selectedProviderId: String?,
    scaffoldPadding: PaddingValues,
    onChangeProvider: (String) -> Unit,
    onToggleProvider: (SearchProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = remember {
        if (providers !is Async.Success) return@remember -1

        providers.data.indexOfFirst { it.id == selectedProviderId }
    }

    val listState = rememberLazyGridState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))

    AsyncAnimatedContent(
        targetState = providers,
        modifier = modifier.fillMaxSize(),
        loadingContent = { SearchProvidersLoading(modifier = Modifier.padding(scaffoldPadding)) },
        errorContent = { state ->
            SearchProvidersError(
                message = state.cause?.stackTraceToString() ?: state.message.asString(),
                modifier = Modifier.padding(scaffoldPadding)
            )
        },
    ) { data ->
        if (data().isEmpty()) {
            EmptyDataMessage(
                modifier = Modifier.padding(scaffoldPadding),
                emojiHeader = "😕",
                title = stringResource(R.string.label_search_empty_providers),
                description = stringResource(R.string.description_search_empty_providers),
            )
        } else {
            SearchProvidersList(
                providers = data(),
                selectedProviderId = selectedProviderId,
                scaffoldPadding = scaffoldPadding,
                onChangeProvider = onChangeProvider,
                onToggleProvider = onToggleProvider,
                listState = listState,
            )
        }
    }
}

@Composable
private fun SearchProvidersList(
    providers: List<SearchProvider>,
    selectedProviderId: String?,
    scaffoldPadding: PaddingValues,
    onChangeProvider: (String) -> Unit,
    onToggleProvider: (SearchProvider) -> Unit,
    listState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier,
        state = listState,
        contentPadding = scaffoldPadding,
        columns = getAdaptiveGridCellsCount(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ViewLabelHeader(label = stringResource(id = LocaleR.string.get_search_results_from))
        }

        items(
            providers,
            key = { item -> item.id },
        ) { item ->
            SearchProviderBlock(
                provider = item,
                isSelected = item.id == selectedProviderId,
                onClick = { onChangeProvider(item.id) },
                onToggle = { onToggleProvider(item) },
            )
        }
    }
}

@Composable
private fun SearchProvidersError(
    message: String,
    modifier: Modifier = Modifier,
) {
    EmptyDataMessage(
        modifier = modifier.padding(horizontal = 15.dp),
        title = stringResource(R.string.search_failed_to_load_search_providers),
        description = message,
        icon = {
            Icon(
                painter = painterResource(UiCommonR.drawable.round_error_outline_24),
                contentDescription = stringResource(LocaleR.string.error_icon_content_desc),
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.error.copy(0.6f),
            )
        },
    )
}

@Composable
private fun SearchProvidersLoading(modifier: Modifier = Modifier) {
    val elevation = Elevations.LEVEL_3

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Placeholder(
            elevation = elevation,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 5.dp)
                .height(14.dp)
                .width(180.dp)
        )

        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            ) {
                Placeholder(
                    elevation = elevation,
                    modifier = Modifier
                        .size(40.dp)
                )

                Column {
                    Placeholder(
                        elevation = elevation,
                        modifier = Modifier
                            .height(12.dp)
                            .width(150.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Placeholder(
                        elevation = elevation,
                        modifier = Modifier
                            .height(12.dp)
                            .width(80.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SearchProvidersLoadingPreview() {
    FlixclusiveTheme {
        Surface {
            EmptyDataMessage(
                emojiHeader = "😕",
                title = stringResource(R.string.label_search_empty_providers),
                description = stringResource(R.string.description_search_empty_providers),
            )
        }
    }
}
