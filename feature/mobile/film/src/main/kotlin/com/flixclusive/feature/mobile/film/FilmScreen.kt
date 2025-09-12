package com.flixclusive.feature.mobile.film

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.navigation.navargs.FilmScreenNavArgs
import com.flixclusive.core.navigation.navargs.GenreWithBackdrop
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.feature.mobile.film.component.BackdropImage
import com.flixclusive.feature.mobile.film.component.BriefDetails
import com.flixclusive.feature.mobile.film.component.CollapsibleDescription
import com.flixclusive.feature.mobile.film.component.ContentTabs
import com.flixclusive.feature.mobile.film.component.FilmScreenTopBar
import com.flixclusive.feature.mobile.film.component.HeaderButtons
import com.flixclusive.feature.mobile.film.component.LibraryListSheet
import com.flixclusive.feature.mobile.film.util.FilmScreenUtils
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class
)
@Composable
internal fun FilmScreen(
    navigator: FilmScreenNavigator,
    navArgs: FilmScreenNavArgs,
    viewModel: FilmScreenViewModel = hiltViewModel()
) {

}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun FilmScreenContent(
    navigator: FilmScreenNavigator,
    watchProgress: WatchProgress?,
    showFilmTitles: Boolean,
    uiState: FilmUiState,
    metadata: Film,
    query: () -> String,
    libraryListStates: () -> List<LibraryListAndState>,
    onQueryChange: (String) -> Unit,
    toggleOnLibrary: (Int) -> Unit,
    createLibrary: (String, String?) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.windowWidthSizeClass.isCompact ||
        windowSizeClass.windowWidthSizeClass.isMedium

    val configuration = LocalConfiguration.current

    val backdropAspectRatio = remember(usePortraitView) { getAspectRatio(usePortraitView) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    var appBarContainerAlpha by remember { mutableFloatStateOf(0f) }
    var isLibrarySheetOpen by remember { mutableStateOf(false) }

    val tabs = remember(metadata) { FilmScreenUtils.getTabs(metadata) }
    val (currentTabSelected, onTabChange) = rememberSaveable(tabs.size) { mutableStateOf(tabs.firstOrNull()) }

    // Extra cards to show based on the selected tab
    val extraFilmCards: List<FilmSearchItem>? = remember(currentTabSelected, metadata) {
        when (currentTabSelected) {
            ContentTabType.MoreLikeThis -> metadata.recommendations
            ContentTabType.Collections -> (metadata as Movie).collection?.films
            else -> null
        }
    }

    val canScrollUpTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    // Scroll to top when canScrollUpTop is true and back is pressed
    BackHandler(
        enabled = canScrollUpTop && uiState.screenState == FilmScreenState.Success
    ) {
        scope.launch {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    // Get the scroll offset of the first item to change the TopAppBar's background alpha
    LaunchedEffect(listState, configuration) {
        snapshotFlow {
            Triple(
                first = listState.firstVisibleItemScrollOffset.toFloat(),
                second = listState.firstVisibleItemIndex,
                third = configuration.screenWidthDp.toFloat()
            )
        }.collect { (offset, index, screenWidth) ->
            val headerHeight = screenWidth / backdropAspectRatio
            val coercedOffset = offset.coerceIn(0f, headerHeight)

            appBarContainerAlpha =
                when {
                    index == 0 && headerHeight > coercedOffset -> coercedOffset / headerHeight
                    else -> 1F
                }
        }
    }

    Scaffold(
        modifier = Modifier
            .padding(LocalGlobalScaffoldPadding.current),
        topBar = {
            FilmScreenTopBar(
                title = metadata.title,
                onNavigate = navigator::goBack,
                containerAlpha = { appBarContainerAlpha },
            )
        }
    ) {
        AnimatedContent(
            modifier = Modifier,
            targetState = uiState.screenState,
            label = "FilmScreenContent"
        ) { state ->
            when (state) {
                FilmScreenState.Loading -> {
                    // TODO: Add placeholder screen
                }

                FilmScreenState.Error -> {
                    RetryButton(
                        error = uiState.error?.asString(),
                        modifier = Modifier.fillMaxSize(),
                        onRetry = onRetry
                    )
                }

                FilmScreenState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(getAdaptiveFilmCardWidth()),
                        state = listState
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                contentAlignment = Alignment.TopCenter
                            ) {
                                BackdropImage(
                                    metadata = metadata as FilmMetadata,
                                    modifier = Modifier
                                        .aspectRatio(backdropAspectRatio)
                                )

                                BriefDetails(
                                    metadata = metadata,
                                    onGenreClick = { /*TODO*/ },
                                    providerUsed = uiState.providerUsed,
                                    modifier = Modifier
                                        .aspectRatio(backdropAspectRatio * 0.95f)
                                        .padding(horizontal = DefaultScreenPaddingHorizontal)
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HeaderButtons(
                                metadata = metadata as FilmMetadata,
                                watchProgress = watchProgress,
                                isInLibrary = watchProgress != null,
                                onPlay = {
                                    if (metadata is Movie) {
                                        navigator.playMovie(metadata)
                                    } else if (metadata is TvShow) {
                                        val episodeProgress = watchProgress as? EpisodeProgress
                                        val season = episodeProgress?.seasonNumber ?: 1
                                        val episode = episodeProgress?.episodeNumber ?: 1

                                        navigator.playEpisode(season, episode, metadata)
                                    }
                                },
                                onAddToLibrary = { isLibrarySheetOpen = true },
                                onToggleDownload = {
                                    // TODO: Implement download
                                    context.showToast(context.getString(LocaleR.string.coming_soon))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DefaultScreenPaddingHorizontal)
                                    .padding(top = 10.dp)
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleDescription(
                                metadata = metadata as FilmMetadata,
                                modifier = Modifier
                                    .padding(horizontal = DefaultScreenPaddingHorizontal)
                                    .padding(top = 25.dp)
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContentTabs(
                                tabs = tabs,
                                currentTabSelected = tabs.indexOf(currentTabSelected),
                                onTabChange = { onTabChange(tabs[it]) },
                                modifier = Modifier
                                    .padding(top = 20.dp, bottom = 10.dp)
                            )
                        }

                        if (currentTabSelected?.isOnFilmsSection == true && extraFilmCards != null) {
                            items(
                                items = extraFilmCards,
                                key = { film -> film.identifier }
                            ) { film ->
                                FilmCard(
                                    isShowingTitle = showFilmTitles,
                                    film = film,
                                    onClick = navigator::openFilmScreen,
                                    onLongClick = navigator::previewFilm,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLibrarySheetOpen) {
        LibraryListSheet(
            query = query,
            libraryListStates = libraryListStates,
            onQueryChange = onQueryChange,
            toggleOnLibrary = toggleOnLibrary,
            createLibrary = createLibrary,
            onDismissRequest = { isLibrarySheetOpen = false }
        )
    }
}

/**
 * Obtains aspect ratio based on current width size class
 * from compose adaptive
 *
 * @param usePortraitView Determines if screen width is compact or medium
 * */
private fun getAspectRatio(usePortraitView: Boolean) =
    when {
        usePortraitView -> 0.8f / 1f
        else -> 16f / 6f
    }


@Preview
@Composable
private fun FilmScreenBasePreview() {
    val navigator = object : FilmScreenNavigator {
        override fun openFilmScreen(film: Film) {}
        override fun openGenreScreen(genre: GenreWithBackdrop) {}
        override fun previewFilm(film: Film) {}
        override fun playMovie(movie: Film) {}
        override fun playEpisode(episode: Episode, film: Film) {}
        override fun playEpisode(season: Int, episode: Int, film: Film) {}
        override fun goBack() {}

    }
    var uiState by remember { mutableStateOf(FilmUiState(isLoading = false, providerUsed = "Netflix")) }
    val metadata = remember {
        DummyDataForPreview.getMovie(
            overview = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.

                Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
            """.trimIndent(),
        ).copy(adult = true)
    }

    var query by remember { mutableStateOf("") }

    var lists by remember {
        val film = if (Random.nextBoolean()) {
            DummyDataForPreview.getMovie().toDBFilm()
        } else {
            null
        }

        val items = if (film != null) {
            listOf(
                LibraryListItemWithMetadata(
                    item = LibraryListItem(listId = 1, filmId = film.id),
                    metadata = film,
                ),
            )
        } else {
            emptyList()
        }

        val list = List(20) {
            LibraryListAndState(
                listWithItems = LibraryListWithItems(
                    items = items,
                    list = LibraryList(
                        id = it,
                        name = "List $it",
                        ownerId = 1,
                        description = "Description $it",
                    ),
                ),
                containsFilm = Random.nextBoolean(),
            )
        }

        mutableStateOf(list)
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            FilmScreenContent(
                uiState = uiState,
                metadata = metadata,
                onRetry = {},
                navigator = navigator,
                showFilmTitles = false,
                toggleOnLibrary = {},
                libraryListStates = { lists },
                query = { query },
                onQueryChange = { query = it },
                createLibrary = { name, description ->
                    lists = lists + LibraryListAndState(
                        listWithItems = LibraryListWithItems(
                            items = emptyList(),
                            list = LibraryList(
                                id = lists.size + 1,
                                name = name,
                                ownerId = 1,
                                description = description,
                            ),
                        ),
                        containsFilm = false,
                    )
                },
                watchProgress = MovieProgress(
                    filmId = metadata.identifier,
                    ownerId = 0,
                    progress = 500L,
                    status = WatchStatus.WATCHING,
                    duration = 6000L,
                ),
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun FilmScreenCompactLandscapePreview() {
    FilmScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun FilmScreenMediumPortraitPreview() {
    FilmScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun FilmScreenMediumLandscapePreview() {
    FilmScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun FilmScreenExtendedPortraitPreview() {
    FilmScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun FilmScreenExtendedLandscapePreview() {
    FilmScreenBasePreview()
}
