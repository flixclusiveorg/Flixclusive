package com.flixclusive.feature.mobile.media

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.navigation.navargs.MediaScreenNavArgs
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.extensions.isWidthCompact
import com.flixclusive.core.presentation.mobile.extensions.isWidthMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.media.component.BackdropImage
import com.flixclusive.feature.mobile.media.component.BriefDetails
import com.flixclusive.feature.mobile.media.component.CollapsibleDescription
import com.flixclusive.feature.mobile.media.component.ContentTabs
import com.flixclusive.feature.mobile.media.component.EpisodeOptionsBottomSheet
import com.flixclusive.feature.mobile.media.component.HeaderButtons
import com.flixclusive.feature.mobile.media.component.LibraryListSheet
import com.flixclusive.feature.mobile.media.component.MediaScreenPlaceholder
import com.flixclusive.feature.mobile.media.component.MediaScreenTopBar
import com.flixclusive.feature.mobile.media.component.seriesContent
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaScreen
import com.flixclusive.feature.mobile.media.util.MediaScreenUtils
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun MediaScreen(
    navigator: NavigatorMediaScreen,
    navArgs: MediaScreenNavArgs,
    viewModel: MediaScreenViewModel = hiltViewModel<MediaScreenViewModel, MediaScreenViewModel.Factory>(
        creationCallback = { it.create(navArgs = navArgs.media) }
    ),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val watchProgress by viewModel.watchProgress.collectAsStateWithLifecycle()
    val seasonToDisplay by viewModel.seasonToDisplay.collectAsStateWithLifecycle()
    val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
    val librarySheetQuery by viewModel.librarySheetQuery.collectAsStateWithLifecycle()
    val libraryListStates by viewModel.libraryLists.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.trackerError.collect {
            snackbarHostState.showSnackbar(it.asString(context))
        }
    }

    MediaScreenContent(
        isLibraryInitiallyOpened = navArgs.isTogglingLibrary,
        navigator = navigator,
        showMediaTitles = showMediaTitles,
        uiState = uiState,
        metadata = metadata ?: navArgs.media,
        watchProgress = watchProgress,
        seasonToDisplay = seasonToDisplay,
        snackbarHostState = snackbarHostState,
        query = { librarySheetQuery },
        searchResults = { searchResults },
        libraryListStates = { libraryListStates },
        onQueryChange = viewModel::onLibrarySheetQueryChange,
        onSeasonChange = viewModel::onSeasonChange,
        toggleOnLibrary = viewModel::toggleOnLibrary,
        toggleEpisodeOnLibrary = viewModel::toggleEpisodeOnLibrary,
        onRetry = viewModel::onRetry,
        onRetryFetchSeason = viewModel::onRetryFetchSeason,
        onRetryFetchLists = viewModel::onRetryFetchLibraries,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun MediaScreenContent(
    navigator: NavigatorMediaScreen,
    isLibraryInitiallyOpened: Boolean,
    showMediaTitles: Boolean,
    uiState: MediaUiState,
    metadata: MediaMetadata,
    watchProgress: WatchProgress?,
    seasonToDisplay: Async<SeasonWithProgress>?,
    query: () -> String,
    libraryListStates: () -> Async<List<LibraryListAndState>>,
    searchResults: () -> Async<List<LibraryListAndState>>,
    onQueryChange: (String) -> Unit,
    onSeasonChange: (Season) -> Unit,
    toggleOnLibrary: (String, LibraryListAndState) -> Unit,
    toggleEpisodeOnLibrary: (EpisodeWithProgress) -> Unit,
    onRetry: () -> Unit,
    onRetryFetchSeason: () -> Unit,
    onRetryFetchLists: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.isWidthCompact || windowSizeClass.isWidthMedium

    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width

    val backdropAspectRatio = remember(usePortraitView) { getBackdropAspectRatio(usePortraitView) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val seasonsListState = rememberLazyListState()

    var appBarContainerAlpha by remember { mutableFloatStateOf(0f) }
    var isLibrarySheetOpen by remember { mutableStateOf(isLibraryInitiallyOpened) }
    var longClickedEpisode by remember { mutableStateOf<EpisodeWithProgress?>(null) }
    var showDefaultProviderDialog by remember { mutableStateOf(false) }

    val tabs = remember(metadata) { MediaScreenUtils.getTabs(metadata) }
    val (currentTabSelected, onTabChange) = rememberSaveable(tabs.size) { mutableStateOf(tabs.firstOrNull()) }

    // Items to show based on the selected tab
    val extraMediaCards: List<MediaMetadata>? = remember(currentTabSelected, metadata) {
        when (currentTabSelected) {
            ContentTabType.MoreLikeThis -> metadata.recommendations
            ContentTabType.Collections -> (metadata as Movie).collection?.parts
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
        enabled = canScrollUpTop && uiState.screenState == MediaScreenState.Success,
    ) {
        scope.launch {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    // Get the scroll offset of the first item to change the TopAppBar's background alpha
    LaunchedEffect(listState, windowInfo, uiState.screenState) {
        snapshotFlow {
            Pair(
                listState.firstVisibleItemScrollOffset.toFloat() to listState.firstVisibleItemIndex,
                screenWidth.toFloat() to uiState.screenState,
            )
        }.collect {
            val (offset, index) = it.first
            val (screenWidth, screenState) = it.second
            val headerHeight = screenWidth / backdropAspectRatio
            val coercedOffset = offset.coerceIn(0f, headerHeight)

            appBarContainerAlpha =
                when {
                    screenState != MediaScreenState.Success -> 1F
                    index == 0 && headerHeight > coercedOffset -> coercedOffset / headerHeight
                    else -> 1F
                }
        }
    }

    Scaffold(
        modifier = modifier
            .padding(LocalGlobalScaffoldPadding.current),
        topBar = {
            MediaScreenTopBar(
                title = metadata.title,
                onNavigate = navigator::navigateBack,
                containerAlpha = { appBarContainerAlpha },
            )
        },
    ) {
        AnimatedContent(
            modifier = Modifier,
            targetState = uiState.screenState,
        ) { state ->
            when (state) {
                MediaScreenState.Loading -> {
                    MediaScreenPlaceholder()
                }

                MediaScreenState.Error -> {
                    RetryButton(
                        error = uiState.error?.asString(),
                        modifier = Modifier.fillMaxSize(),
                        onRetry = onRetry,
                    )
                }

                MediaScreenState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(
                            getAdaptiveMediaCardWidth() *
                                (if (currentTabSelected?.isOnEpisodesSection == true) 3.5f else 1f),
                        ),
                        state = listState,
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                BackdropImage(
                                    metadata = metadata,
                                    modifier = Modifier
                                        .aspectRatio(backdropAspectRatio),
                                )

                                BriefDetails(
                                    metadata = metadata,
                                    onProviderClick = {
                                        if (uiState.provider == null) return@BriefDetails

                                        navigator.showProviderDetailsSheet(uiState.provider)
                                    },
                                    onGenreClick = { genre ->
                                        genre.catalog?.let(navigator::navigateToSeeAllScreen)
                                    },
                                    provider = uiState.provider,
                                    modifier = Modifier
                                        .aspectRatio(backdropAspectRatio * 0.95f)
                                        .padding(horizontal = DefaultScreenPaddingHorizontal),
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val isInLibrary by remember {
                                derivedStateOf {
                                    when (val state = libraryListStates()) {
                                        is Async.Loading -> Async.Loading
                                        is Async.Failure -> Async.Failure(state.message)
                                        is Async.Success -> Async.Success(state.data.fastAny { it.containsMedia })
                                    }
                                }
                            }

                            HeaderButtons(
                                metadata = metadata,
                                watchProgress = watchProgress,
                                isInLibrary = isInLibrary,
                                onPlay = { navigator.showLinkLoaderSheet(metadata) },
                                onAddToLibrary = { isLibrarySheetOpen = true },
                                onToggleDownload = {
                                    // TODO: Implement download
                                    context.showToast(resources.getString(LocaleR.string.coming_soon))
                                },
                                onRetryFetchLists = onRetryFetchLists,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DefaultScreenPaddingHorizontal)
                                    .padding(top = 20.dp),
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleDescription(
                                metadata = metadata,
                                isCollapsible = tabs.isNotEmpty(),
                                modifier = Modifier
                                    .padding(horizontal = DefaultScreenPaddingHorizontal)
                                    .padding(top = 30.dp),
                            )
                        }

                        if (tabs.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ContentTabs(
                                    tabs = tabs,
                                    currentTabSelected = tabs.indexOf(currentTabSelected),
                                    onTabChange = { onTabChange(tabs[it]) },
                                    modifier = Modifier
                                        .padding(top = 20.dp, bottom = 10.dp),
                                )
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
                            }
                        }

                        if (metadata is Show &&
                            currentTabSelected?.isOnEpisodesSection == true &&
                            seasonToDisplay != null &&
                            uiState.selectedSeason != null
                        ) {
                            seriesContent(
                                listState = seasonsListState,
                                selectedSeason = uiState.selectedSeason,
                                seasons = metadata.seasons,
                                seasonToDisplay = seasonToDisplay,
                                onSeasonChange = onSeasonChange,
                                onRetry = onRetryFetchSeason,
                                onClick = { episode -> navigator.showLinkLoaderSheet(metadata, episode = episode) },
                                onLongClick = { longClickedEpisode = it },
                            )
                        }

                        if (currentTabSelected?.isOnMediasSection == true && extraMediaCards != null) {
                            items(
                                items = extraMediaCards,
                                key = { media -> media.id },
                            ) { media ->
                                MediaCard(
                                    isShowingTitle = showMediaTitles,
                                    media = media,
                                    onClick = navigator::navigateToMediaScreen,
                                    onLongClick = navigator::showMediaPreviewBottomSheet,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(),
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
            libraryListStates = {
                if (query().isBlank()) {
                    libraryListStates()
                } else {
                    searchResults()
                }
            },
            onQueryChange = onQueryChange,
            toggleOnLibrary = toggleOnLibrary,
            onDismissRequest = { isLibrarySheetOpen = false },
            snackbarHostState = snackbarHostState,
        )
    }

    longClickedEpisode?.let { episode ->
        EpisodeOptionsBottomSheet(
            episode = episode,
            onToggleWatchStatus = {
                toggleEpisodeOnLibrary(episode)
                longClickedEpisode = null
            },
            onDismissRequest = { longClickedEpisode = null },
        )
    }

    if (showDefaultProviderDialog) {
        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning),
            contentDescription = stringResource(R.string.default_provider_content_desc),
            description = stringResource(R.string.default_provider_message),
            onConfirm = { showDefaultProviderDialog = false },
            dismissButtonLabel = null,
        )
    }
}

/**
 * Obtains aspect ratio based on current width size class
 * from compose adaptive
 *
 * @param usePortraitView Determines if screen width is compact or medium
 * */
internal fun getBackdropAspectRatio(usePortraitView: Boolean) =
    when {
        usePortraitView -> MediaCover.Poster.ratio
        else -> 16f / 6f
    }

@Preview
@Composable
private fun MediaScreenBasePreview() {
    val navigator = object : NavigatorMediaScreen {
        override fun navigateToMediaScreen(media: MediaMetadata, isTogglingLibrary: Boolean) {}

        override fun showMediaPreviewBottomSheet(media: MediaMetadata) {}

        override fun showLinkLoaderSheet(media: MediaMetadata, episode: Episode?) {}

        override fun showProviderDetailsSheet(provider: ProviderMetadata) {}

        override fun navigateBack() {}

        override fun navigateToSeeAllScreen(item: Catalog) {}
    }
    var uiState by remember {
        mutableStateOf(
            MediaUiState(
                isLoading = false,
                provider = null,
                selectedSeason = 2,
            ),
        )
    }
    val metadata: MediaMetadata = remember {
        DummyDataForPreview
            .getShow(
                overview = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.

                Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                """.trimIndent(),
            ).copy(adult = true)
    }

    var query by remember { mutableStateOf("") }

    var lists by remember {
        val media = if (Random.nextBoolean()) {
            DummyDataForPreview.getMovie().toDBMedia()
        } else {
            null
        }

        val items = if (media != null) {
            listOf(
                LibraryListItemWithMetadata(
                    item = LibraryListItem(listId = "1", mediaId = media.id),
                    metadata = media,
                    externalIds = emptyList()
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
                        id = it.toString(),
                        name = "List $it",
                        ownerId = "preview-user",
                        description = "Description $it",
                    ),
                ),
                containsMedia = Random.nextBoolean(),
            )
        }

        mutableStateOf(list)
    }

    val watchProgress: WatchProgress = remember(metadata) {
        if (metadata is Show) {
            val duration = 900L + Random.nextInt(1200, 6000)

            EpisodeProgress(
                ownerId = "preview-user",
                mediaId = metadata.id,
                progress = Random.nextLong(900, duration),
                duration = duration,
                seasonNumber = 2,
                episodeNumber = 1,
                status = WatchStatus.WATCHING,
            )
        } else {
            MovieProgress(
                ownerId = "preview-user",
                mediaId = metadata.id,
                progress = 5400L,
                duration = 7200L,
                status = WatchStatus.WATCHING,
            )
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            MediaScreenContent(
                navigator = navigator,
                isLibraryInitiallyOpened = false,
                showMediaTitles = false,
                uiState = uiState,
                metadata = metadata,
                watchProgress = watchProgress,
                seasonToDisplay = remember(uiState.selectedSeason) {
                    if (metadata is Show) {
                        val season =
                            metadata.seasons.first { it.number == (uiState.selectedSeason ?: 1) } as Season.Full
                        Async.Success(
                            data = SeasonWithProgress(
                                season = season,
                                episodes = season.episodes.fastMap { episode ->
                                    val duration = 900L + Random.nextInt(1200, 6000)

                                    EpisodeWithProgress(
                                        episode = episode,
                                        watchProgress = EpisodeProgress(
                                            id = episode.number.toLong(),
                                            ownerId = "preview-user",
                                            mediaId = metadata.id,
                                            progress = Random.nextLong(900, duration),
                                            duration = duration,
                                            seasonNumber = season.number,
                                            episodeNumber = episode.number,
                                            status = WatchStatus.WATCHING,
                                        ),
                                    )
                                },
                            ),
                        )
                    } else {
                        null
                    }
                },
                query = { query },
                libraryListStates = { Async.Success(lists) },
                searchResults = {
                    Async.Success(lists.filter { it.list.name.contains(query, ignoreCase = true) })
                },
                onQueryChange = { query = it },
                onSeasonChange = { uiState = uiState.copy(selectedSeason = it.number) },
                toggleOnLibrary = { _, _ -> },
                toggleEpisodeOnLibrary = {},
                onRetry = {},
                onRetryFetchLists = {},
                onRetryFetchSeason = {}
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun MediaScreenCompactLandscapePreview() {
    MediaScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun MediaScreenMediumPortraitPreview() {
    MediaScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun MediaScreenMediumLandscapePreview() {
    MediaScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun MediaScreenExtendedPortraitPreview() {
    MediaScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun MediaScreenExtendedLandscapePreview() {
    MediaScreenBasePreview()
}
