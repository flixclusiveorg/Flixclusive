package com.flixclusive.core.presentation.mobile.components.film

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.R
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.topbar.DefaultNavigationIcon
import com.flixclusive.core.presentation.mobile.components.topbar.TwoRowsTopAppBar
import com.flixclusive.core.presentation.mobile.components.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.util.FilmType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun FilmsGridScreen(
    title: String,
    films: List<Film>,
    pagingState: PagingDataState,
    isShowingFilmCardTitle: Boolean,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyGridState = rememberLazyGridState(),
    currentFilter: FilmType? = null,
    onRetry: () -> Unit = {},
    onFilterChange: (FilmType) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    val canScrollUpTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    BackHandler(enabled = canScrollUpTop) {
        scope.launch {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    LaunchedEffect(pagingState) {
        if (pagingState is PagingDataState.Error) {
            snackbarHostState.showSnackbar(
                message = pagingState.error.asString(context),
                actionLabel = context.getString(LocaleR.string.ok),
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            GridTopBar(
                title = title,
                onNavigationClick = onNavigationClick,
                selected = currentFilter,
                scrollBehavior = scrollBehavior,
                onFilterChange = { onFilterChange(it) },
            )
        },
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            contentPadding = it,
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = films,
                key = { _, film -> film.identifier },
            ) { _, film ->
                FilmCard(
                    modifier = Modifier.fillMaxWidth(),
                    isShowingTitle = isShowingFilmCardTitle,
                    film = film,
                    onClick = onFilmClick,
                    onLongClick = onFilmLongClick,
                )
            }

            if (pagingState.isLoading) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier = Modifier
                            .padding(3.dp)
                            .fillMaxSize(),
                    )
                }
            }

            if (films.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val error = if (pagingState is PagingDataState.Error) {
                        pagingState.error
                    } else {
                        null
                    }

                    RetryButton(
                        error = error?.asString(),
                        shouldShowError = pagingState.isError,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GridTopBar(
    title: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: FilmType? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onFilterChange: (FilmType) -> Unit,
) {
    TwoRowsTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        titleTextStyle = MaterialTheme.typography.titleLarge,
        navigationIcon = { DefaultNavigationIcon(onClick = onNavigationClick) },
        collapsedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        windowInsets = TopAppBarDefaults.windowInsets,
        colors = TopAppBarDefaults.largeTopAppBarColors(),
        actions = {},
        scrollBehavior = scrollBehavior,
    ) {
        TypeFilter(
            selected = selected ?: FilmType.MOVIE,
            onClick = { onFilterChange(it) },
            modifier = Modifier
                .padding(bottom = 10.dp),
        )
    }
}

@Composable
private fun TypeFilter(
    selected: FilmType,
    onClick: (FilmType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        AdaptiveIcon(
            painter = painterResource(UiCommonR.drawable.filter_list),
            contentDescription = stringResource(R.string.type_filter),
        )

        Spacer(modifier = Modifier.width(1.dp))

        FilmType.entries.forEach { filter ->
            val buttonColors = when (selected == filter) {
                true -> ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                )

                false -> ButtonDefaults.outlinedButtonColors(
                    contentColor = LocalContentColor.current.copy(0.6f),
                )
            }

            OutlinedButton(
                onClick = { onClick(filter) },
                enabled = selected != filter,
                colors = buttonColors,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .height(getAdaptiveDp(29.dp))
                    .widthIn(min = getAdaptiveDp(55.dp))
                    .graphicsLayer {
                        alpha = if (selected == filter) 1f else 0.6f
                    },
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 3.dp,
                ),
            ) {
                Text(
                    text = stringResource(filter.stringId),
                    style = getAdaptiveTextStyle(
                        style = AdaptiveTextStyle.SemiEmphasized(),
                        size = 12.sp,
                    ),
                    color = LocalContentColor.current.copy(0.8f),
                )
            }
        }
    }
}

private val FilmType.stringId: Int
    get() = when (this) {
        FilmType.MOVIE -> LocaleR.string.movie
        FilmType.TV_SHOW -> LocaleR.string.tv_show
    }

private val dummyFilms
    @Composable get() = remember {
        List(50) {
            val id = it + 1

            Movie(
                tmdbId = it,
                imdbId = "tt4154796$id",
                title = "Film #$id",
                posterImage = null,
                backdropImage = "/orjiB3oUIsyz60hoEqkiGpy5CeO.jpg",
                homePage = null,
                id = null,
                providerId = "TMDB",
            )
        }.toImmutableList()
    }

@Preview
@Composable
private fun FilmsGridScreenPreview() {
    FlixclusiveTheme {
        Surface {
            FilmsGridScreen(
                title = "Trending",
                films = dummyFilms,
                isShowingFilmCardTitle = false,
                currentFilter = FilmType.MOVIE,
                pagingState = PagingDataState.Loading,
                onFilmClick = {},
                onFilmLongClick = {},
                onNavigationClick = {},
                onFilterChange = {},
            )
        }
    }
}

@Preview
@Composable
private fun FilmsGridScreenErrorPreview() {
    FlixclusiveTheme {
        Surface {
            FilmsGridScreen(
                title = "Trending",
                films = emptyList(),
                isShowingFilmCardTitle = false,
                currentFilter = FilmType.MOVIE,
                pagingState = PagingDataState.Error("An error occurred"),
                onFilmClick = {},
                onFilmLongClick = {},
                onNavigationClick = {},
            )
        }
    }
}
