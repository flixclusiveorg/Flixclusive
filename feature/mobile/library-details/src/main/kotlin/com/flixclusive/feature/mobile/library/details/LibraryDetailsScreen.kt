package com.flixclusive.feature.mobile.library.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.presentation.common.components.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.CommonPullToRefreshBox
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterOnlyNearTopScrollBehavior
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState
import com.flixclusive.feature.mobile.library.common.component.LibraryFilterRow
import com.flixclusive.feature.mobile.library.common.util.selectionBorder
import com.flixclusive.feature.mobile.library.details.component.ScreenHeader
import com.flixclusive.feature.mobile.library.details.component.topbar.LibraryDetailsTopBar
import com.flixclusive.feature.mobile.library.details.component.topbar.TopTitleAlphaEasing
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import java.util.Date
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private enum class LibraryDetailsScreenState {
    Loading,
    Error,
    Success,
}

@Destination<ExternalModuleGraph>(navArgs = LibraryDetailsNavArgs::class)
@Composable
fun LibraryDetailsScreen(
    navArgs: LibraryDetailsNavArgs,
    navigator: NavigatorLibraryDetailsScreen,
    viewModel: LibraryDetailsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val library by viewModel.library.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchItems by viewModel.searchItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.trackerError.collect {
            snackbarHostState.showSnackbar(it.asString(context))
        }
    }

    LibraryDetailsScreenContent(
        library = library,
        tracker = navArgs.tracker,
        snackbarHostState = snackbarHostState,
        uiState = { uiState },
        items = {
            if (searchQuery.isNotEmpty() && uiState.isShowingSearchBar) {
                searchItems
            } else {
                viewModel.items
            }
        },
        searchQuery = { searchQuery },
        selectedItems = { viewModel.selectedItems },
        onGoBack = navigator::navigateBack,
        onViewMedia = navigator::navigateToMediaScreen,
        onLongClickItem = {
            if (!uiState.isMultiSelecting) {
                navigator.showMediaPreviewBottomSheet(it.toMediaMetadata())
            } else {
                viewModel.onToggleSelect(it)
            }
        },
        onRefresh = { viewModel.initialize(isRefreshing = true) },
        onStartMultiSelecting = viewModel::onStartMultiSelecting,
        onToggleSelect = viewModel::onToggleSelect,
        onUpdateFilter = viewModel::onUpdateFilter,
        onRemoveSelection = viewModel::onRemoveSelection,
        onQueryChange = viewModel::onQueryChange,
        onUnselectAll = viewModel::onUnselectAll,
        onToggleSearchBar = viewModel::onToggleSearchBar,
        paginate = viewModel::paginate,
    )
}

@Composable
private fun LibraryDetailsScreenContent(
    library: LibraryList,
    tracker: ProviderMetadata?,
    uiState: () -> LibraryDetailsUiState,
    paginate: () -> Unit,
    searchQuery: () -> String,
    items: () -> Set<LibraryListItemWithMetadata>,
    selectedItems: () -> Set<LibraryListItemWithMetadata>,
    onRefresh: () -> Unit,
    onGoBack: () -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onViewMedia: (MediaMetadata) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (LibraryListItemWithMetadata) -> Unit,
    onLongClickItem: (LibraryListItemWithMetadata) -> Unit,
    onUpdateFilter: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val refreshState = rememberPullToRefreshState()
    val scrollBehavior = rememberEnterOnlyNearTopScrollBehavior()

    val isRefreshing by remember {
        derivedStateOf { uiState().isRefreshing }
    }

    val selectCount by remember {
        derivedStateOf { selectedItems().size }
    }

    val isListEmpty by remember {
        derivedStateOf {
            items().isEmpty()
        }
    }

    val screenState by remember {
        derivedStateOf {
            val state = uiState()
            when {
                state.pagingState.isLoading && items().isEmpty() && state.currentPage == 1 -> {
                    LibraryDetailsScreenState.Loading
                }

                state.pagingState.isError && items().isEmpty() -> {
                    LibraryDetailsScreenState.Error
                }

                else -> {
                    LibraryDetailsScreenState.Success
                }
            }
        }
    }

    var showDeleteSelectionAlert by remember { mutableStateOf(false) }

    CommonPullToRefreshBox(
        isRefreshing = isRefreshing,
        state = refreshState,
        onRefresh = onRefresh
    ) {
        Scaffold(
            modifier = modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val topBarState = remember(uiState().isMultiSelecting, uiState().isShowingSearchBar) {
                    when {
                        uiState().isMultiSelecting -> LibraryTopBarState.Selecting
                        uiState().isShowingSearchBar -> LibraryTopBarState.Searching
                        else -> LibraryTopBarState.DefaultSubScreen
                    }
                }

                LibraryDetailsTopBar(
                    topBarState = topBarState,
                    scrollBehavior = scrollBehavior,
                    isListEmpty = isListEmpty,
                    onGoBack = onGoBack,
                    selectCount = selectCount,
                    searchQuery = searchQuery,
                    onToggleSearchBar = onToggleSearchBar,
                    onQueryChange = onQueryChange,
                    onRemoveSelection = { showDeleteSelectionAlert = true },
                    onUnselectAll = onUnselectAll,
                    title = {
                        val title = if (topBarState == LibraryTopBarState.Selecting) {
                            stringResource(LocaleR.string.count_selection_format, selectCount)
                        } else {
                            library.name
                        }

                        Text(
                            text = title,
                            style = getTopBarHeadlinerTextStyle(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                alpha = when (topBarState) {
                                    LibraryTopBarState.Selecting -> 1f
                                    else -> TopTitleAlphaEasing.transform(scrollBehavior.state.collapsedFraction)
                                }
                            },
                        )
                    },
                    infoContent = {
                        ScreenHeader(
                            library = library,
                            tracker = tracker,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                        )
                    },
                    filterContent = {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
                            )

                            LibraryFilterRow(
                                isListEditable = !isListEmpty && !uiState().isMultiSelecting,
                                selected = { uiState().selectedFilter },
                                onStartSelecting = onStartMultiSelecting,
                                onUpdate = onUpdateFilter,
                                enabled = tracker == null,
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            AnimatedContent(
                screenState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    LibraryDetailsScreenState.Error -> {
                        val pagingError = remember { uiState().pagingState as PagingState.Error }
                        RetryButton(
                            error = pagingError.error.asString(),
                            onRetry = paginate,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = isListEmpty && !uiState().pagingState.isLoading,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { isEmpty ->
                            if (isEmpty) {
                                EmptyDataMessage(
                                    modifier = Modifier
                                        .padding(paddingValues)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface),
                                )
                            } else {
                                NonEmptyScreen(
                                    uiState = uiState,
                                    selectedItems = selectedItems,
                                    itemsProvider = items,
                                    scaffoldPadding = paddingValues,
                                    onViewMedia = onViewMedia,
                                    onLongClickItem = onLongClickItem,
                                    onToggleSelect = onToggleSelect,
                                    paginate = paginate,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSelectionAlert) {
        val alertDescription = stringResource(LocaleR.string.warn_delete_selected_libraries_format)

        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            description = alertDescription,
            onConfirm = onRemoveSelection,
            onDismiss = { showDeleteSelectionAlert = false },
        )
    }
}

@Composable
private fun NonEmptyScreen(
    uiState: () -> LibraryDetailsUiState,
    selectedItems: () -> Set<LibraryListItemWithMetadata>,
    itemsProvider: () -> Set<LibraryListItemWithMetadata>,
    scaffoldPadding: PaddingValues,
    onViewMedia: (MediaMetadata) -> Unit,
    onLongClickItem: (LibraryListItemWithMetadata) -> Unit,
    onToggleSelect: (LibraryListItemWithMetadata) -> Unit,
    paginate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyGridState()
    val items by remember {
        derivedStateOf { itemsProvider().toList() }
    }

    val placeholders by remember {
        derivedStateOf {
            if (uiState().pagingState.isLoading) 20 else 0
        }
    }

    LaunchedEffect(listState, uiState, paginate) {
        snapshotFlow {
            listState.shouldPaginate() && uiState().pagingState.isIdle
        }.collect { canPaginate ->
            if (canPaginate) {
                paginate()
            }
        }
    }

    LazyVerticalGrid(
        state = listState,
        columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
        contentPadding = scaffoldPadding,
        modifier = modifier.padding(top = 10.dp),
    ) {
        Snapshot.withoutReadObservation {
            listState.requestScrollToItem(
                index = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }

        items(
            items = items,
            key = { it.mediaId }
        ) { item ->
            val media = item.toMediaMetadata()
            val isSelected by remember {
                derivedStateOf { selectedItems().contains(item) }
            }

            MediaCard(
                media = media,
                onClick = {
                    if (uiState().isMultiSelecting) {
                        onToggleSelect(item)
                    } else {
                        onViewMedia(it)
                    }
                },
                onLongClick = { onLongClickItem(item) },
                modifier = Modifier
                    .animateItem()
                    .selectionBorder(
                        isSelected = isSelected,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
            )
        }

        items(
            count = placeholders,
            key = { "flixclusive-placeholder-card-$it" },
        ) {
            MediaCardPlaceholder(
                modifier = Modifier
                    .animateItem()
                    .padding(3.dp)
            )
        }

        if (uiState().pagingState.isExhausted && itemsProvider().isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(LocaleR.string.label_list_exhausted_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            }
        }

        if (uiState().pagingState.isError && itemsProvider().isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ).background(
                            color = MaterialTheme.colorScheme.error.copy(0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    RetryButton(
                        error = (uiState().pagingState as PagingState.Error).error.asString(),
                        onRetry = paginate,
                        modifier = Modifier
                            .padding(vertical = 30.dp)
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.padding(LocalGlobalScaffoldPadding.current))
        }
    }
}

@Preview
@Composable
private fun LibraryDetailsScreenBasePreview() {
    val sampleList =
        remember {
            LibraryList(
                id = "1",
                ownerId = "preview-user",
                name = "Best horror movies",
                description = "A curation of the best horror movies out there. Feel free to browse my list :D",
                createdAt = Date(),
                updatedAt = Date(),
            )
        }

    var uiState by remember { mutableStateOf(LibraryDetailsUiState()) }
    var searchQuery by remember { mutableStateOf("") }
    val medias = remember { mutableStateListOf<LibraryListItemWithMetadata>() }
    val selectedItems = remember { mutableStateSetOf<LibraryListItemWithMetadata>() }

    val safeItems by remember {
        derivedStateOf {
            val list =
                if (searchQuery.isNotEmpty()) {
                    medias.filter {
                        it.metadata.title.contains(searchQuery, true) ||
                            it.metadata.overview?.contains(searchQuery, true) == true
                    }
                } else {
                    medias
                }

            val sortedList =
                list.sortedWith(
                    compareBy<LibraryListItemWithMetadata>(
                        selector = {
                            when (uiState.selectedFilter) {
                                is LibrarySort.Name -> it.metadata.title
                                is LibrarySort.Added -> it.item.createdAt.time
                                else -> throw Error()
                            }
                        },
                    ).let { comparator ->
                        if (uiState.selectedFilter.ascending) comparator else comparator.reversed()
                    },
                )

            sortedList.toSet()
        }
    }

    ProvideAsyncImagePreviewHandler {
        FlixclusiveTheme {
            Surface {
                LibraryDetailsScreenContent(
                    library = sampleList,
                    tracker = DummyDataForPreview.getProviderMetadata(),
                    uiState = { uiState },
                    paginate = {
                        if (uiState.currentPage == 5) {
                            uiState = uiState.copy(pagingState = PagingState.Exhausted)
                            return@LibraryDetailsScreenContent
                        }

                        uiState = uiState.copy(pagingState = PagingState.Loading)
                        medias.addAll(
                            List(15) {
                                val media = DummyDataForPreview.getMovie(
                                    id = "${it + 1}",
                                    title = "MediaMetadata $it",
                                )

                                LibraryListItemWithMetadata(
                                    metadata = media.toDBMedia(),
                                    item = LibraryListItem(
                                        id = it.toString(),
                                        mediaId = media.id,
                                        listId = sampleList.id,
                                        createdAt = Date(System.currentTimeMillis() - it * 10000000L),
                                    ),
                                    externalIds = emptyList()
                                )
                            },
                        )
                        uiState = uiState.copy(
                            pagingState = PagingState.Idle,
                            currentPage = uiState.currentPage + 1
                        )
                    },
                    searchQuery = { searchQuery },
                    items = { safeItems },
                    selectedItems = { selectedItems },
                    onRefresh = {},
                    onGoBack = {},
                    onRemoveSelection = {
                        selectedItems.forEach { media ->
                            medias.removeIf { it.metadata.id == media.metadata.id }
                        }
                    },
                    onStartMultiSelecting = { uiState = uiState.copy(isMultiSelecting = true) },
                    onUnselectAll = {
                        uiState = uiState.copy(isMultiSelecting = false)
                        selectedItems.clear()
                    },
                    onViewMedia = {},
                    onQueryChange = { searchQuery = it },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                    onToggleSelect = {
                        if (selectedItems.contains(it)) {
                            selectedItems.remove(it)
                        } else {
                            selectedItems.add(it)
                        }
                    },
                    onLongClickItem = { },
                    onUpdateFilter = {
                        uiState = if (uiState.selectedFilter == it) {
                            uiState.copy(selectedFilter = uiState.selectedFilter.toggleAscending())
                        } else {
                            uiState.copy(selectedFilter = it)
                        }
                    },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LibraryDetailsScreenCompactLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun LibraryDetailsScreenMediumPortraitPreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun LibraryDetailsScreenMediumLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun LibraryDetailsScreenExtendedPortraitPreview() {
    LibraryDetailsScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun LibraryDetailsScreenExtendedLandscapePreview() {
    LibraryDetailsScreenBasePreview()
}
