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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.components.film.FilmCardPlaceholder
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.feature.mobile.search.R
import com.flixclusive.model.film.Film
import com.flixclusive.core.strings.R as LocaleR

private enum class SearchFilmsGridViewState {
    EMPTY,
    NON_EMPTY,
    ERROR;

    val isEmpty: Boolean get() = this == EMPTY
    val isNonEmpty: Boolean get() = this == NON_EMPTY
    val isError: Boolean get() = this == ERROR
}

@Composable
internal fun SearchFilmsGridView(
    searchResults: () -> Set<Film>,
    pagingState: () -> PagingState,
    error: UiText?,
    scaffoldPadding: PaddingValues,
    listState: LazyGridState,
    showFilmTitles: Boolean,
    paginateItems: () -> Unit,
    openFilmScreen: (Film) -> Unit,
    previewFilm: (Film) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentState = remember(
        searchResults().isEmpty(),
        pagingState().isError,
        error
    ) {
        when {
            error != null || (pagingState().isError && searchResults().isEmpty()) -> SearchFilmsGridViewState.ERROR
            searchResults().isEmpty() -> SearchFilmsGridViewState.EMPTY
            else -> SearchFilmsGridViewState.NON_EMPTY
        }
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = { fadeIn() togetherWith  fadeOut() },
        ) { state ->
            if (state.isError) {
                val errorMsg = error ?: (pagingState() as PagingState.Error).error

                RetryButton(
                    error = errorMsg.asString(),
                    onRetry = paginateItems,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )
            } else if (state.isEmpty) {
                SearchEmptyState(modifier = Modifier.padding(scaffoldPadding))
            } else {
                SearchNonEmptyState(
                    results = searchResults,
                    listState = listState,
                    pagingState = pagingState,
                    scaffoldPadding = scaffoldPadding,
                    showFilmTitles = showFilmTitles,
                    openFilmScreen = openFilmScreen,
                    previewFilm = previewFilm,
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
    results: () -> Set<Film>,
    listState: LazyGridState,
    pagingState: () -> PagingState,
    scaffoldPadding: PaddingValues,
    showFilmTitles: Boolean,
    openFilmScreen: (Film) -> Unit,
    previewFilm: (Film) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(getAdaptiveFilmCardWidth()),
        state = listState,
        contentPadding = scaffoldPadding,
    ) {
        items(
            results().size,
            key = { results().elementAt(it).id },
        ) {
            val film = results().elementAt(it)

            FilmCard(
                film = film,
                isShowingTitle = showFilmTitles,
                onClick = openFilmScreen,
                onLongClick = previewFilm,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth(),
            )
        }

        if (pagingState().isLoading) {
            items(20) {
                FilmCardPlaceholder(
                    isShowingTitle = showFilmTitles,
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
    }
}
