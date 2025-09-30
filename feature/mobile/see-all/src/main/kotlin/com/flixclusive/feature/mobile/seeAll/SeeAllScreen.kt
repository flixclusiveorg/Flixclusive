package com.flixclusive.feature.mobile.seeAll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.components.film.FilmCardPlaceholder
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.model.film.Film
import com.flixclusive.model.provider.Catalog
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Destination(
    navArgsDelegate = SeeAllScreenNavArgs::class,
)
@Composable
internal fun SeeAllScreen(
    navigator: SeeAllScreenNavigator,
    args: SeeAllScreenNavArgs,
    viewModel: SeeAllViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showFilmTitles by viewModel.showFilmTitles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    SeeAllScreenContent(
        items = {
            if (searchQuery.isNotBlank() && uiState.isSearching) {
                viewModel.items
                    .filter {
                        it.title.contains(searchQuery, ignoreCase = true)
                    }.toImmutableSet()
            } else {
                viewModel.items
            }
        },
        uiState = uiState,
        showFilmTitles = showFilmTitles,
        catalog = args.catalog,
        searchQuery = { searchQuery },
        onQueryChange = viewModel::onQueryChange,
        previewFilm = navigator::previewFilm,
        onGoBack = navigator::goBack,
        openFilmScreen = navigator::openFilmScreen,
        onToggleSearchBar = viewModel::onToggleSearch,
        paginate = viewModel::paginate,
    )
}

@Composable
private fun SeeAllScreenContent(
    items: () -> ImmutableSet<Film>,
    uiState: SeeAllUiState,
    showFilmTitles: Boolean,
    catalog: Catalog,
    searchQuery: () -> String,
    onQueryChange: (String) -> Unit,
    previewFilm: (Film) -> Unit,
    onGoBack: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    openFilmScreen: (Film) -> Unit,
    paginate: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    val updatedPaginateItems by rememberUpdatedState(paginate)
    LaunchedEffect(listState, uiState.canPaginate) {
        snapshotFlow { uiState.canPaginate && listState.shouldPaginate() }
            .distinctUntilChanged()
            .collect { shouldPaginate ->
                if (shouldPaginate) {
                    updatedPaginateItems()
                }
            }
    }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CommonTopBarWithSearch(
                title = catalog.name,
                onNavigate = onGoBack,
                scrollBehavior = scrollBehavior,
                isSearching = uiState.isSearching,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(getAdaptiveFilmCardWidth()),
            contentPadding = it,
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items().size,
                key = {
                    val film = items().elementAt(it)
                    film.identifier
                },
            ) {
                val film = items().elementAt(it)

                FilmCard(
                    isShowingTitle = showFilmTitles,
                    film = film,
                    onClick = openFilmScreen,
                    onLongClick = previewFilm,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                )
            }

            if (uiState.pagingState.isLoading) {
                items(20) {
                    FilmCardPlaceholder(
                        modifier = Modifier
                            .padding(3.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            if (uiState.pagingState is PagingDataState.Error) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    RetryButton(
                        error = uiState.pagingState.error.asString(),
                        onRetry = paginate,
                        modifier = Modifier.aspectRatio(FilmCover.Backdrop.ratio),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SeeAllScreenBasePreview() {
    val films = remember {
        (1..20)
            .map {
                DummyDataForPreview.getFilm(
                    id = it.toString(),
                    title = "Film $it",
                )
            }.toImmutableSet()
    }
    var searchQuery by remember { mutableStateOf("") }
    var uiState by remember {
        mutableStateOf(
            SeeAllUiState(
                page = 1,
                maxPage = 1,
                canPaginate = false,
                pagingState = PagingDataState.Success(isExhausted = true),
            ),
        )
    }

    FlixclusiveTheme {
        Surface {
            SeeAllScreenContent(
                items = {
                    if (searchQuery.isBlank()) {
                        films
                    } else {
                        films
                            .filter {
                                it.title.contains(searchQuery, ignoreCase = true)
                            }.toImmutableSet()
                    }
                },
                uiState = uiState,
                showFilmTitles = true,
                catalog = remember {
                    object : Catalog() {
                        override val canPaginate: Boolean = true
                        override val image: String? = null
                        override val name: String = "Netflix"
                        override val url: String = ""
                    }
                },
                searchQuery = { searchQuery },
                onQueryChange = { searchQuery = it },
                previewFilm = {},
                onGoBack = {},
                openFilmScreen = {},
                onToggleSearchBar = {
                    uiState = uiState.copy(isSearching = it)
                },
                paginate = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SeeAllScreenCompactLandscapePreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SeeAllScreenMediumPortraitPreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SeeAllScreenMediumLandscapePreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SeeAllScreenExtendedPortraitPreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SeeAllScreenExtendedLandscapePreview() {
    SeeAllScreenBasePreview()
}
