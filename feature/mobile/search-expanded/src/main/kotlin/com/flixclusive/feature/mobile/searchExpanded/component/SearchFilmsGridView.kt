package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.mobile.component.LARGE_ERROR
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.SMALL_ERROR
import com.flixclusive.core.ui.mobile.component.film.FilmCard
import com.flixclusive.core.ui.mobile.component.film.FilmCardPlaceholder
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenViewModel
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.film.Film
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SearchFilmsGridView(
    modifier: Modifier = Modifier,
    viewModel: SearchExpandedScreenViewModel,
    listState: LazyGridState,
    appSettings: AppSettings,
    openFilmScreen: (Film) -> Unit,
    previewFilm: (Film) -> Unit,
) {

    val errorHeight = remember(viewModel.searchResults) {
        when {
            viewModel.searchResults.isEmpty() -> LARGE_ERROR
            else -> SMALL_ERROR
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(110.dp),
        state = listState,
        contentPadding = PaddingValues(),
        modifier = modifier
    ) {
        items(viewModel.searchResults) { film ->
            FilmCard(
                modifier = Modifier.fillMaxSize(),
                film = film,
                isShowingTitle = appSettings.isShowingFilmCardTitle,
                onClick = openFilmScreen,
                onLongClick = previewFilm
            )
        }

        if(viewModel.pagingState.isLoading) {
            items(20) {
                FilmCardPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    isShowingTitle = appSettings.isShowingFilmCardTitle
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            RetryButton(
                modifier = Modifier
                    .height(errorHeight)
                    .fillMaxWidth(),
                shouldShowError = viewModel.pagingState.isError,
                error = viewModel.error?.asString()
                    ?: stringResource(LocaleR.string.error_on_search),
                onRetry = viewModel::paginateItems
            )
        }
    }
}