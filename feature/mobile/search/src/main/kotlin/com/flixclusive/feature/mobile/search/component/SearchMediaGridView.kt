package com.flixclusive.feature.mobile.search.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.feature.mobile.search.R
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.core.strings.R as LocaleR

private enum class SearchMediasGridViewState {
    EMPTY,
    NON_EMPTY,
    ERROR;

    val isEmpty: Boolean get() = this == EMPTY
    val isError: Boolean get() = this == ERROR
}

@Composable
internal fun SearchMediasGridView(
    searchResults: () -> Set<MediaMetadata>,
    pagingState: () -> PagingState,
    scaffoldPadding: PaddingValues,
    listState: LazyGridState,
    showMediaTitles: Boolean,
    paginateItems: () -> Unit,
    openMediaScreen: (MediaMetadata) -> Unit,
    previewMedia: (MediaMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentState by remember {
        derivedStateOf {
            val state = pagingState()
            val results = searchResults()

            when {
                state.isError && results.isEmpty() -> SearchMediasGridViewState.ERROR
                results.isEmpty() && (state.isIdle || state.isExhausted) -> SearchMediasGridViewState.EMPTY
                else -> SearchMediasGridViewState.NON_EMPTY
            }
        }
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize()
        ) { state ->
            if (state.isError) {
                RetryButton(
                    error = (pagingState() as? PagingState.Error)?.error?.asString(),
                    onRetry = paginateItems,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )
            } else if (state.isEmpty) {
                SearchEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            } else {
                SearchNonEmptyState(
                    results = searchResults,
                    listState = listState,
                    pagingState = pagingState,
                    scaffoldPadding = scaffoldPadding,
                    showMediaTitles = showMediaTitles,
                    openMediaScreen = openMediaScreen,
                    previewMedia = previewMedia,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyDataMessage(
            emojiHeader = "🫥",
            title = stringResource(R.string.search_empty_results_title),
            description = stringResource(R.string.search_empty_results_desc),
        )
    }
}

@Composable
private fun SearchNonEmptyState(
    results: () -> Set<MediaMetadata>,
    listState: LazyGridState,
    pagingState: () -> PagingState,
    scaffoldPadding: PaddingValues,
    showMediaTitles: Boolean,
    openMediaScreen: (MediaMetadata) -> Unit,
    previewMedia: (MediaMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
        state = listState,
        contentPadding = scaffoldPadding,
        modifier = modifier,
    ) {
        items(
            results().size,
            key = { results().elementAt(it).id },
        ) {
            val media by remember {
                derivedStateOf { results().elementAt(it) }
            }

            MediaCard(
                media = media,
                isShowingTitle = showMediaTitles,
                onClick = openMediaScreen,
                onLongClick = previewMedia,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth(),
            )
        }

        if (pagingState().isLoading) {
            items(20) {
                MediaCardPlaceholder(
                    isShowingTitle = showMediaTitles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                )
            }
        }

        if (pagingState().isError) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyDataMessage(
                    modifier = Modifier.padding(vertical = 15.dp),
                    title = stringResource(LocaleR.string.something_went_wrong),
                    description = stringResource(R.string.search_failed_pagination_generic_message),
                    icon = {},
                )
            }
        }

        if (pagingState().isExhausted) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(LocaleR.string.label_list_exhausted_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            }
        }
    }
}
