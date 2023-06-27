package com.flixclusive.presentation.common.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.film.FilmCard
import com.flixclusive.presentation.film.FilmCardPlaceholder
import com.flixclusive.presentation.main.BOTTOM_NAVIGATION_BAR_PADDING

@Composable
fun VerticalGridWithFiltersScreen(
    screenTitle: String,
    listState: LazyGridState = rememberLazyGridState(),
    pagingState: PagingState = PagingState.IDLE,
    currentFilter: FilmType = FilmType.MOVIE,
    films: List<Film> = emptyList(),
    onRetry: () -> Unit,
    onFilterChange: (FilmType) -> Unit = {},
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit = {},
    onNavigationIconClick: (() -> Unit)? = null,
) {
    var shouldShowFilterSheet by rememberSaveable { mutableStateOf(true) }
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    val errorHeight = remember(films) {
        if(films.isEmpty()) {
            LARGE_ERROR
        } else {
            SMALL_ERROR
        }
    }

    LaunchedEffect(listIsAtTop, shouldShowTopBar) {
        shouldShowFilterSheet = if(shouldShowTopBar && !listIsAtTop) false
        else shouldShowTopBar
    }

    Scaffold(
         topBar = {
            AnimatedVisibility(
                visible = shouldShowTopBar,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                VerticalGridHeaderWithFilterIcon(
                    headerTitle = screenTitle,
                    shouldOpenFilterSheet = shouldShowFilterSheet,
                    currentFilterSelected = currentFilter,
                    onNavigationIconClick = onNavigationIconClick,
                    onFilterChange = onFilterChange,
                    onFilterClick = {
                        shouldShowFilterSheet = !shouldShowFilterSheet
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        val topPadding by animateDpAsState(
            targetValue = if(listIsAtTop) innerPadding.calculateTopPadding() else 0.dp
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            state = listState,
            modifier = Modifier.padding(top = topPadding)
        ) {
            itemsIndexed(
                items = films,
                key = { i, film ->
                    film.id * i
                }
            ) { _, film ->
                FilmCard(
                    modifier = Modifier
                        .fillMaxSize(),
                    film = film,
                    onClick = onFilmClick,
                    onLongClick = onFilmLongClick
                )
            }

            if(pagingState == PagingState.LOADING || pagingState == PagingState.PAGINATING) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreenWithButton(
                    modifier = Modifier
                        .height(errorHeight)
                        .fillMaxWidth(),
                    shouldShowError = pagingState == PagingState.ERROR,
                    error = stringResource(id = R.string.pagination_error_message),
                    onRetry = onRetry
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(
                    modifier = Modifier
                        .padding(bottom = BOTTOM_NAVIGATION_BAR_PADDING)
                )
            }
        }
    }
}

@Composable
fun VerticalGridScreen(
    modifier: Modifier = Modifier,
    screenTitle: String,
    listState: LazyGridState = rememberLazyGridState(),
    films: List<Film> = emptyList(),
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit = {},
    onNavigationIconClick: (() -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        state = listState,
        modifier = modifier,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            VerticalGridHeader(
                headerTitle = screenTitle,
                onNavigationIconClick = onNavigationIconClick
            )
        }

        itemsIndexed(
            items = films,
            key = { i, film ->
                film.id * i
            }
        ) { _, film ->
            FilmCard(
                modifier = Modifier
                    .fillMaxSize(),
                film = film,
                onClick = onFilmClick,
                onLongClick = onFilmLongClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier
                    .padding(bottom = BOTTOM_NAVIGATION_BAR_PADDING)
            )
        }
    }
}