package com.flixclusive.presentation.mobile.common.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.mobile.common.composables.film.FilmCard
import com.flixclusive.presentation.mobile.common.composables.film.FilmCardPlaceholder
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.utils.LazyListUtils.isAtTop
import com.flixclusive.presentation.utils.LazyListUtils.isScrollingUp

@Composable
fun FilmsGridScreen(
    modifier: Modifier = Modifier,
    screenTitle: String,
    films: List<Film>,
    isShowingFilmCardTitle: Boolean,
    listState: LazyGridState = rememberLazyGridState(),
    pagingState: PagingState = PagingState.IDLE,
    currentFilter: FilmType? = null,
    onRetry: () -> Unit = {},
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onFilterChange: ((FilmType) -> Unit)? = null,
    onNavigationIconClick: (() -> Unit)? = null,
) {
    var shouldShowFilterSheet by rememberSaveable { mutableStateOf(true) }
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    val errorHeight = remember(films) {
        if (films.isEmpty()) {
            LARGE_ERROR
        } else {
            SMALL_ERROR
        }
    }

    LaunchedEffect(listIsAtTop, shouldShowTopBar) {
        shouldShowFilterSheet = if (shouldShowTopBar && !listIsAtTop) false
        else shouldShowTopBar
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = shouldShowTopBar,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                FilmsGridHeader(
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
            targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
            label = ""
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
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
                        .fillMaxWidth(),
                    shouldShowTitle = isShowingFilmCardTitle,
                    film = film,
                    onClick = onFilmClick,
                    onLongClick = onFilmLongClick
                )
            }

            if (pagingState == PagingState.LOADING || pagingState == PagingState.PAGINATING) {
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
        }
    }
}

@Composable
private fun FilmsGridHeader(
    headerTitle: String,
    shouldOpenFilterSheet: Boolean,
    currentFilterSelected: FilmType? = null,
    onFilterClick: () -> Unit,
    onNavigationIconClick: (() -> Unit)? = null,
    onFilterChange: ((FilmType) -> Unit)? = null,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .drawBehind {
                drawRect(surfaceColor)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(65.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(onNavigationIconClick != null) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = stringResource(R.string.navigate_up)
                    )
                }
            }

            Text(
                text = headerTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1F)
                    .padding(start = LABEL_START_PADDING)
            )

            if (currentFilterSelected != null) {
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .padding(end = LABEL_START_PADDING)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.filter),
                        contentDescription = stringResource(R.string.filter_button)
                    )
                }
            }
        }

        if(currentFilterSelected != null) {
            AnimatedVisibility(
                visible = shouldOpenFilterSheet
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(surfaceColor)
                        }
                ) {
                    FilmTypeFilters(
                        currentFilterSelected = currentFilterSelected,
                        onFilterChange = onFilterChange!!
                    )
                }
            }
        }
    }
}