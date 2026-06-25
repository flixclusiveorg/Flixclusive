package com.flixclusive.feature.mobile.home

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.components.isLoadingWithDelay
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.ViewModelUtil.activityHiltViewModel
import com.flixclusive.core.presentation.mobile.components.CommonPullToRefreshBox
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.extensions.isWidthCompact
import com.flixclusive.core.presentation.mobile.extensions.isWidthMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.home.components.CatalogProvidersBottomSheet
import com.flixclusive.feature.mobile.home.components.CatalogRow
import com.flixclusive.feature.mobile.home.components.ContinueWatchingRow
import com.flixclusive.feature.mobile.home.components.HomeMediaHeader
import com.flixclusive.feature.mobile.home.components.HomeScreenTopBar
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.Repository
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>(start = true)
@Composable
internal fun HomeScreen(
    navigator: NavigatorHome,
    viewModel: HomeViewModel = activityHiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
    val catalogProviders by viewModel.catalogProviders.collectAsStateWithLifecycle()
    val continueWatchingItems by viewModel.continueWatchingItems.collectAsStateWithLifecycle()

    HomeScreenContent(
        navigator = navigator,
        uiState = uiState,
        showMediaTitles = { showMediaTitles },
        providers = { catalogProviders },
        continueWatchingItems = { continueWatchingItems },
        onToggle = viewModel::onToggleProvider,
        paginate = viewModel::paginate,
        onRetry = viewModel::initialize,
        onRefresh = { viewModel.initialize(isRefreshing = true) },
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun HomeScreenContent(
    navigator: NavigatorHome,
    uiState: HomeUiState,
    showMediaTitles: () -> Boolean,
    providers: () -> Async<List<CatalogProvider>>,
    onToggle: (CatalogProvider) -> Unit,
    paginate: (CatalogWithPagingState) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    continueWatchingItems: () -> List<WatchProgressWithMetadata>,
) {
    var appBarContainerAlpha by remember { mutableFloatStateOf(0f) }
    var isSheetOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerSize.width

    val backdropAspectRatio = getBackdropAspectRatio()

    // Get the scroll offset of the first item to change the TopAppBar's background alpha
    LaunchedEffect(listState, windowInfo) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemScrollOffset.toFloat(),
                listState.firstVisibleItemIndex,
                screenWidth.toFloat()
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

    CommonPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(),
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeScreenTopBar(
                    title = stringResource(LocaleR.string.home),
                    containerAlpha = { appBarContainerAlpha },
                    onFilterClick = { isSheetOpen = true },
                    enableFilterButton = { providers().let { it is Async.Success && it.data.isNotEmpty() } },
                )
            },
        ) {
            AsyncAnimatedContent(
                targetState = uiState.catalogs,
                modifier = Modifier.fillMaxSize(),
                loadingContent = {
                    LoadingScreen(
                        message = stringResource(R.string.label_loading_catalogs),
                        modifier = Modifier
                            .padding(it)
                            .padding(LocalGlobalScaffoldPadding.current),
                    )
                },
                errorContent = { error ->
                    RetryButton(
                        error = error.message.asString(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                            .padding(LocalGlobalScaffoldPadding.current),
                        onRetry = onRetry,
                    )
                }
            ) { data ->
                AnimatedContent(
                    targetState = data().isEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxSize(),
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyScreenContent(
                            openAddProviderScreen = navigator::navigateToAddProviderScreen,
                        )
                    } else {
                        val updatedData by rememberUpdatedState(data)
                        val catalogValues by remember {
                            derivedStateOf { updatedData().values.toList() }
                        }

                        NonEmptyScreenContent(
                            navigator = navigator,
                            showMediaTitles = showMediaTitles,
                            paginate = paginate,
                            continueWatchingItems = continueWatchingItems,
                            catalogs = catalogValues,
                            headerItem = uiState.itemHeader,
                            listState = listState,
                        )
                    }
                }
            }
        }
    }

    if (isSheetOpen) {
        CatalogProvidersBottomSheet(
            onDismiss = { isSheetOpen = false },
            providers = providers(),
            onToggle = onToggle
        )
    }
}

@Composable
private fun EmptyScreenContent(
    openAddProviderScreen: () -> Unit
) {
    EmptyDataMessage(
        modifier = Modifier.fillMaxSize(),
        title = stringResource(R.string.empty_catalogs_label),
        description = stringResource(R.string.empty_catalog_providers_msg)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Text(
                text = "🤔",
                modifier = Modifier.padding(bottom = 6.dp),
                style = MaterialTheme.typography.displayMedium
                    .copy(
                        shadow = Shadow(offset = Offset(4F, 5F)),
                        color = MaterialTheme.colorScheme.primary
                    ).asAdaptiveTextStyle(),
            )

            OutlinedButton(
                onClick = openAddProviderScreen,
                modifier = Modifier,
            ) {
                Text(text = stringResource(LocaleR.string.add_providers))
            }
        }
    }
}

@Composable
private fun NonEmptyScreenContent(
    navigator: NavigatorHome,
    catalogs: List<CatalogWithPagingState>,
    headerItem: Async<MediaMetadata>,
    showMediaTitles: () -> Boolean,
    listState: LazyListState,
    paginate: (CatalogWithPagingState) -> Unit,
    continueWatchingItems: () -> List<WatchProgressWithMetadata>,
) {
    val scope = rememberCoroutineScope()

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
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            HomeMediaHeader(
                media = headerItem,
                onMediaClick = navigator::navigateToMediaScreen,
                onMediaLongClick = navigator::showMediaPreviewBottomSheet,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Spacer(modifier = Modifier.size(50.dp))
        }

        if (continueWatchingItems().isNotEmpty()) {
            item {
                ContinueWatchingRow(
                    items = continueWatchingItems,
                    showCardTitle = showMediaTitles(),
                    onSeeMoreClick = navigator::showMediaPreviewBottomSheet,
                    onItemClick = { navigator.showLinkLoaderSheet(it.toMediaMetadata()) },
                )
            }
        }

        items(
            catalogs,
            key = { it.catalog.hashCode() }
        ) { data ->
            CatalogRow(
                catalog = data.catalog,
                pagingState = data.state,
                items = { data.medias },
                onMediaClick = navigator::navigateToMediaScreen,
                showTitles = showMediaTitles(),
                onMediaLongClick = navigator::showMediaPreviewBottomSheet,
                paginate = { paginate(data) },
                onSeeAllItems = { navigator.navigateToSeeAllScreen(item = data.catalog) },
            )
        }
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    AnimatedVisibility(
        visible = isLoadingWithDelay(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            GradientCircularProgressIndicator(
                modifier = Modifier.size(getAdaptiveDp(48.dp)),
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun getBackdropAspectRatio(): Float {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.isWidthCompact || windowSizeClass.isWidthMedium

    return when {
        windowSizeClass.isWidthMedium -> 2.1f / 3f
        usePortraitView -> MediaCover.Poster.ratio
        else -> 16f / 6f
    }
}

@Preview
@Composable
private fun HomeScreenBasePreview() {
    val resources = LocalResources.current

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            val loadingState = 0
            val errorState = 1
            val readyState = 2

            val dummyNavigator = object : NavigatorHome {
                override fun navigateToMediaScreen(media: MediaMetadata, isTogglingLibrary: Boolean) {}

                override fun navigateToSeeAllScreen(item: Catalog) {}

                override fun navigateBack() {}

                override fun showMediaPreviewBottomSheet(media: MediaMetadata) {}

                override fun showLinkLoaderSheet(media: MediaMetadata, episode: Episode?) {}

                override fun navigateToAddProviderScreen(initialSelectedRepositoryFilter: Repository?) {}
            }

            var previewState by remember { mutableIntStateOf(readyState) }

            when (previewState) {
                loadingState -> {
                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(),
                        showMediaTitles = { true },
                        providers = { Async.Loading },
                        continueWatchingItems = { emptyList() },
                        onToggle = { },
                        paginate = { },
                        onRetry = { },
                        onRefresh = { },
                    )
                }

                errorState -> {
                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(
                            catalogs = Async.Failure(stringResource(LocaleR.string.something_went_wrong))
                        ),
                        showMediaTitles = { true },
                        providers = { Async.Failure(resources.getString(LocaleR.string.something_went_wrong)) },
                        continueWatchingItems = { emptyList() },
                        onToggle = { },
                        paginate = { },
                        onRetry = { },
                        onRefresh = { },
                    )
                }

                readyState -> {
                    val dummyCatalogs = remember {
                        listOf(
                            Catalog(
                                name = "Popular Movies",
                                url = "popular_movies",
                                canPaginate = true,
                                providerId = "dummy_provider",
                            ),
                            Catalog(
                                name = "Trending TV Shows",
                                url = "trending_tv",
                                canPaginate = true,
                                providerId = "dummy_provider",
                            ),
                            Catalog(
                                name = "Action Movies",
                                url = "action_movies",
                                canPaginate = false,
                                providerId = "dummy_provider",
                            ),
                        )
                    }

                    val dummyItems = remember {
                        List(8) { index ->
                            DummyDataForPreview.getMedia(
                                id = "movie_$index",
                                title = "Popular Movie ${index + 1}",
                                mediaType = MediaType.MOVIE,
                            )
                        }.toSet()
                    }

                    val dummyPagingStates = remember {
                        mapOf(
                            "popular_movies" to CatalogWithPagingState(
                                page = 1,
                                state = PagingState.Exhausted,
                                medias = dummyItems,
                                catalog = dummyCatalogs[0],
                            ),
                            "trending_tv" to CatalogWithPagingState(
                                page = 1,
                                state = PagingState.Exhausted,
                                medias = dummyItems,
                                catalog = dummyCatalogs[1],
                            ),
                            "action_movies" to CatalogWithPagingState(
                                page = 1,
                                state = PagingState.Error(LocaleR.string.end_of_list),
                                medias = dummyItems,
                                catalog = dummyCatalogs[2],
                            ),
                        )
                    }

                    val dummyHeaderMedia = remember {
                        DummyDataForPreview.getMedia(
                            id = "header_media",
                            title = "Featured Movie",
                            overview = "An amazing featured media t...",
                            mediaType = MediaType.MOVIE,
                        )
                    }

                    val continueWatchingItems = remember {
                        listOf(
                            MovieProgressWithMetadata(
                                watchData = MovieProgress(
                                    id = 0,
                                    ownerId = "preview-user",
                                    mediaId = "continue_1",
                                    progress = 3600000L, // 1 hour in milliseconds
                                    status = WatchStatus.WATCHING,
                                ),
                                media = DummyDataForPreview
                                    .getMedia(
                                        id = "continue_1",
                                        title = "Continue Movie",
                                        mediaType = MediaType.MOVIE,
                                    ).toDBMedia(),
                                externalIds = emptyList(),
                            ),
                            EpisodeProgressWithMetadata(
                                watchData = EpisodeProgress(
                                    id = 1,
                                    ownerId = "preview-user",
                                    mediaId = "continue_2",
                                    seasonNumber = 1,
                                    episodeNumber = 3,
                                    progress = 1800000L, // 30 minutes in milliseconds
                                    status = WatchStatus.WATCHING,
                                ),
                                media = DummyDataForPreview
                                    .getMedia(
                                        id = "continue_2",
                                        title = "Continue TV Show",
                                        mediaType = MediaType.SHOW,
                                    ).toDBMedia(),
                                externalIds = emptyList(),
                            ),
                        )
                    }

                    HomeScreenContent(
                        navigator = dummyNavigator,
                        uiState = HomeUiState(
                            itemHeader = Async.Success(dummyHeaderMedia),
                            catalogs = Async.Success(dummyPagingStates),
                        ),
                        showMediaTitles = { true },
                        providers = {
                            Async.Success(
                                List(3) {
                                    CatalogProvider(
                                        isCatalogEnabled = true,
                                        provider = DummyDataForPreview.getProviderMetadata(
                                            id = "provider_$it",
                                            name = "Provider ${it + 1}",
                                        )
                                    )
                                }
                            )
                        },
                        continueWatchingItems = { continueWatchingItems },
                        onToggle = { },
                        paginate = { },
                        onRetry = { },
                        onRefresh = { },
                    )
                }
            }

            // State switcher for preview testing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
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
