package com.flixclusive.core.ui.mobile.component.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.common.pagination.PagingState
import com.flixclusive.core.ui.mobile.component.LARGE_ERROR
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.SMALL_ERROR
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBar
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
fun FilmsGridScreen(
    title: String,
    films: List<Film>,
    isShowingFilmCardTitle: Boolean,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyGridState = rememberLazyGridState(),
    pagingState: com.flixclusive.core.common.pagination.PagingState = com.flixclusive.core.common.pagination.PagingState.IDLE,
    currentFilter: FilmType? = null,
    onRetry: () -> Unit = {},
    onFilterChange: (FilmType) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    var isFilterSheetOpen by rememberSaveable { mutableStateOf(true) }

    val isScrollToTopEnabled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    BackHandler(
        enabled = isScrollToTopEnabled,
    ) {
        scope.launch {
            safeCall { listState.animateScrollToItem(0) }
        }
    }

    val errorHeight =
        remember(films.size) {
            if (films.isEmpty()) {
                LARGE_ERROR
            } else {
                SMALL_ERROR
            }
        }

    val localDensity = LocalDensity.current
    val defaultHeight = CommonTopBarDefaults.getAdaptiveTopBarHeight()
    var topBarHeightPx = remember { mutableFloatStateOf(0f) }
    val topBarOffsetHeightPx = remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
                    val delta = available.y
                    val newOffset = topBarOffsetHeightPx.floatValue + delta
                    topBarOffsetHeightPx.floatValue = newOffset.coerceIn(-topBarHeightPx.floatValue, 0f)
                    // here's the catch: let's pretend we consumed 0 in any case, since we want
                    // LazyColumn to scroll anyway for good UX
                    // We're basically watching scroll without taking it
                    return Offset.Zero
                }
            }
        }

    val topBarHeightInDp by remember {
        derivedStateOf {
            if (topBarHeightPx.floatValue == 0f) {
                return@derivedStateOf defaultHeight
            }

            with(localDensity) { topBarHeightPx.floatValue.toDp() }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(LocalGlobalScaffoldPadding.current)
                .nestedScroll(nestedScrollConnection),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            state = listState,
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "PaddingSpacer",
            ) {
                Spacer(modifier = Modifier.heightIn(topBarHeightInDp),)
            }

            itemsIndexed(
                items = films,
                key = { _, film -> film.identifier }
            ) { _, film ->
                FilmCard(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    isShowingTitle = isShowingFilmCardTitle,
                    film = film,
                    onClick = onFilmClick,
                    onLongClick = onFilmLongClick,
                )
            }

            if (pagingState == com.flixclusive.core.common.pagination.PagingState.LOADING || pagingState == com.flixclusive.core.common.pagination.PagingState.PAGINATING) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier =
                            Modifier
                                .padding(3.dp)
                                .fillMaxSize(),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                RetryButton(
                    modifier =
                        Modifier
                            .height(errorHeight)
                            .fillMaxWidth(),
                    shouldShowError = pagingState == com.flixclusive.core.common.pagination.PagingState.ERROR,
                    error = stringResource(id = LocaleR.string.pagination_error_message),
                    onRetry = onRetry,
                )
            }
        }

        FilmsGridTopBar(
            headerTitle = title,
            isFilterSheetOpen = isFilterSheetOpen,
            currentFilterSelected = currentFilter,
            onNavigationIconClick = onNavigationIconClick,
            onFilterChange = onFilterChange,
            onFilterClick = { isFilterSheetOpen = !isFilterSheetOpen },
            modifier =
                Modifier
                    .offset {
                        IntOffset(0, topBarOffsetHeightPx.floatValue.roundToInt())
                    }.onGloballyPositioned {
                        topBarHeightPx.floatValue = it.size.height.toFloat()
                    },
        )
    }
}

@Composable
private fun FilmsGridTopBar(
    headerTitle: String,
    isFilterSheetOpen: Boolean,
    onFilterClick: () -> Unit,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentFilterSelected: FilmType? = null,
    onFilterChange: ((FilmType) -> Unit)? = null,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier =
            modifier
                .background(surfaceColor),
    ) {
        CommonTopBar(
            title = headerTitle,
            onNavigate = onNavigationIconClick,
            actions = {
                if (currentFilterSelected != null) {
                    IconButton(
                        onClick = onFilterClick,
                        modifier =
                            Modifier
                                .padding(end = 15.dp),
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.filter_list),
                            contentDescription = stringResource(LocaleR.string.filter_button),
                        )
                    }
                }
            },
        )

        if (currentFilterSelected != null) {
            AnimatedVisibility(visible = isFilterSheetOpen) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(surfaceColor),
                ) {
                    FilmTypeFilters(
                        currentFilterSelected = currentFilterSelected,
                        onFilterChange = onFilterChange!!,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FilmsGridScreenPreview() {
    val films =
        remember {
            List(100) {
                Movie(
                    tmdbId = it,
                    imdbId = "tt4154796",
                    title = "Avengers: Endgame",
                    posterImage = null,
                    backdropImage = "/orjiB3oUIsyz60hoEqkiGpy5CeO.jpg",
                    homePage = null,
                    id = null,
                    providerId = "TMDB",
                )
            }
        }

    FlixclusiveTheme {
        Surface {
            FilmsGridScreen(
                title = "Trending",
                films = films,
                isShowingFilmCardTitle = false,
                currentFilter = FilmType.MOVIE,
                onFilmClick = {},
                onFilmLongClick = {},
                onNavigationIconClick = {},
                onFilterChange = {},
            )
        }
    }
}
