package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.components.film.FilmCardPlaceholder
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.model.film.Film
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun SearchFilmsGridView(
    searchResults: ImmutableSet<Film>,
    pagingState: PagingDataState,
    error: UiText?,
    scaffoldPadding: PaddingValues,
    listState: LazyGridState,
    showFilmTitles: Boolean,
    paginateItems: () -> Unit,
    openFilmScreen: (Film) -> Unit,
    previewFilm: (Film) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(getAdaptiveFilmCardWidth()),
        state = listState,
        contentPadding = scaffoldPadding,
        modifier = modifier,
    ) {
        items(
            searchResults.size,
            key = { searchResults.elementAt(it).identifier },
        ) {
            val film = searchResults.elementAt(it)

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

        if (pagingState.isLoading) {
            items(20) {
                FilmCardPlaceholder(
                    isShowingTitle = showFilmTitles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                )
            }
        }

        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RetryButton(
                    error = error.asString(),
                    onRetry = paginateItems,
                    modifier = Modifier
                        .aspectRatio(FilmCover.Poster.ratio)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
