package com.flixclusive.feature.mobile.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.home.components.CatalogRow
import com.flixclusive.feature.mobile.home.components.ContinueWatchingRow
import com.flixclusive.feature.mobile.home.components.DisplayHeader
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Catalog
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch

@Destination
@Composable
internal fun HomeScreen(
    navigator: HomeNavigator,
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showFilmTitles by viewModel.showFilmTitles.collectAsStateWithLifecycle()
    val continueWatchingItems by viewModel.continueWatchingItems.collectAsStateWithLifecycle()
    val catalogs by viewModel.catalogs.collectAsStateWithLifecycle()

    HomeScreenContent(
        navigator = navigator,
        uiState = uiState,
        showFilmTitles = showFilmTitles,
        catalogs = catalogs,
        continueWatchingItems = continueWatchingItems,
        paginate = viewModel::paginate,
        onRetryFetchingHeaderItem = viewModel::loadHomeHeader,
    )
}

@Composable
private fun HomeScreenContent(
    navigator: HomeNavigator,
    uiState: HomeUiState,
    showFilmTitles: Boolean,
    catalogs: List<Catalog>,
    paginate: (Catalog, Int) -> Unit,
    continueWatchingItems: List<WatchProgressWithMetadata>,
    onRetryFetchingHeaderItem: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val canScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    BackHandler(enabled = canScrollToTop) {
        scope.launch {
            runCatching {
                listState.animateScrollToItem(0)
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = LocalGlobalScaffoldPadding.current,
    ) {
        item {
            DisplayHeader(
                film = uiState.itemHeader,
                onFilmClick = navigator::openFilmScreen,
                onFilmLongClick = navigator::previewFilm,
                onRetry = onRetryFetchingHeaderItem,
                error = uiState.itemHeaderError,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (continueWatchingItems.isNotEmpty()) {
            item {
                ContinueWatchingRow(
                    items = continueWatchingItems,
                    showCardTitle = showFilmTitles,
                    onSeeMoreClick = navigator::previewFilm,
                    onItemClick = { navigator.play(it.film) },
                )
            }
        }

        items(
            count = catalogs.size,
            key = { catalogs.elementAt(it).url },
        ) { i ->
            val catalog = catalogs.elementAt(i)
            val pagingState = uiState.pagingStates[catalog.url] ?: return@items
            val items = uiState.items[catalog.url] ?: return@items

            CatalogRow(
                catalog = catalog,
                pagingState = pagingState,
                items = items,
                onFilmClick = navigator::openFilmScreen,
                showTitles = showFilmTitles,
                onFilmLongClick = navigator::previewFilm,
                paginate = { paginate(catalog, it) },
                onSeeAllItems = { navigator.openSeeAllScreen(item = catalog) },
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            val loadingState = 0
            val errorState = 1
            val readyState = 2

            val dummyNavigator = object : HomeNavigator {
                override fun openFilmScreen(film: Film) {}

                override fun openSeeAllScreen(item: Catalog) {}

                override fun goBack() {}

                override fun previewFilm(film: Film) {}

                override fun play(film: Film, episode: Episode?) {}
            }

            var previewState by remember { mutableIntStateOf(readyState) }

            when (previewState) {
                loadingState -> {
                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(),
                        showFilmTitles = true,
                        catalogs = emptyList(),
                        paginate = { _, _ -> },
                        continueWatchingItems = emptyList(),
                        onRetryFetchingHeaderItem = { previewState = readyState },
                    )
                }

                errorState -> {
                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(itemHeaderError = UiText.from(R.string.failed_to_get_header_item)),
                        showFilmTitles = true,
                        catalogs = emptyList(),
                        paginate = { _, _ -> },
                        continueWatchingItems = emptyList(),
                        onRetryFetchingHeaderItem = { previewState = readyState },
                    )
                }

                readyState -> {
                    val dummyCatalogs = remember {
                        listOf(
                            object : Catalog() {
                                override val name: String = "Popular Movies"
                                override val url: String = "popular_movies"
                                override val image: String? = null
                                override val canPaginate: Boolean = true
                            },
                            object : Catalog() {
                                override val name: String = "Trending TV Shows"
                                override val url: String = "trending_tv"
                                override val image: String? = null
                                override val canPaginate: Boolean = true
                            },
                            object : Catalog() {
                                override val name: String = "Action Movies"
                                override val url: String = "action_movies"
                                override val image: String? = null
                                override val canPaginate: Boolean = false
                            },
                        )
                    }

                    val dummyItems = remember {
                        persistentHashMapOf(
                            "popular_movies" to List(8) { index ->
                                DummyDataForPreview.getFilm(
                                    id = "movie_$index",
                                    title = "Popular Movie ${index + 1}",
                                    filmType = FilmType.MOVIE,
                                )
                            }.toPersistentSet(),
                            "trending_tv" to List(6) { index ->
                                DummyDataForPreview.getFilm(
                                    id = "tv_$index",
                                    title = "TV Show ${index + 1}",
                                    filmType = FilmType.TV_SHOW,
                                )
                            }.toPersistentSet(),
                            "action_movies" to List(4) { index ->
                                DummyDataForPreview.getFilm(
                                    id = "action_$index",
                                    title = "Action Film ${index + 1}",
                                    filmType = FilmType.MOVIE,
                                )
                            }.toPersistentSet(),
                        )
                    }

                    val dummyPagingStates = remember {
                        persistentHashMapOf(
                            "popular_movies" to CatalogPagingState(
                                hasNext = true,
                                page = 1,
                                state = PagingDataState.Success(isExhausted = false),
                            ),
                            "trending_tv" to CatalogPagingState(
                                hasNext = true,
                                page = 1,
                                state = PagingDataState.Success(isExhausted = false),
                            ),
                            "action_movies" to CatalogPagingState(
                                hasNext = false,
                                page = 1,
                                state = PagingDataState.Error(com.flixclusive.core.strings.R.string.end_of_list),
                            ),
                        )
                    }

                    val dummyHeaderFilm = remember {
                        DummyDataForPreview.getFilm(
                            id = "header_film",
                            title = "Featured Movie",
                            overview = "An amazing featured film t...",
                            filmType = FilmType.MOVIE,
                        )
                    }

                    val continueWatchingItems = remember {
                        listOf(
                            MovieProgressWithMetadata(
                                watchData = MovieProgress(
                                    id = 0,
                                    ownerId = 1,
                                    filmId = "continue_1",
                                    progress = 3600000L, // 1 hour in milliseconds
                                    status = WatchStatus.WATCHING,
                                ),
                                film = DummyDataForPreview
                                    .getFilm(
                                        id = "continue_1",
                                        title = "Continue Movie",
                                        filmType = FilmType.MOVIE,
                                    ).toDBFilm(),
                            ),
                            EpisodeProgressWithMetadata(
                                watchData = EpisodeProgress(
                                    id = 1,
                                    ownerId = 1,
                                    filmId = "continue_2",
                                    seasonNumber = 1,
                                    episodeNumber = 3,
                                    progress = 1800000L, // 30 minutes in milliseconds
                                    status = WatchStatus.WATCHING,
                                ),
                                film = DummyDataForPreview
                                    .getFilm(
                                        id = "continue_2",
                                        title = "Continue TV Show",
                                        filmType = FilmType.TV_SHOW,
                                    ).toDBFilm(),
                            ),
                        )
                    }

                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(
                            itemHeader = dummyHeaderFilm,
                            items = dummyItems,
                            pagingStates = dummyPagingStates,
                        ),
                        showFilmTitles = true,
                        catalogs = dummyCatalogs,
                        paginate = { catalog, page ->
                            // Simulate pagination without LaunchedEffect
                            println("Paginating ${catalog.name} to page $page")
                        },
                        continueWatchingItems = continueWatchingItems,
                        onRetryFetchingHeaderItem = { previewState = loadingState },
                    )
                }
            }

            // State switcher for preview testing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    val state = when (it) {
                        0 -> loadingState
                        1 -> errorState
                        else -> readyState
                    }

                    Surface(
                        onClick = { previewState = state },
                        color = when (previewState) {
                            state -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "$state state",
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center,
                            color = when (previewState) {
                                state -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun HomeScreenCompactLandscapePreview() {
    HomeScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun HomeScreenMediumPortraitPreview() {
    HomeScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun HomeScreenMediumLandscapePreview() {
    HomeScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun HomeScreenExtendedPortraitPreview() {
    HomeScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun HomeScreenExtendedLandscapePreview() {
    HomeScreenBasePreview()
}
