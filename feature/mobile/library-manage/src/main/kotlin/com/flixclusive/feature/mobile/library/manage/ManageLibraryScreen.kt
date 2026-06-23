package com.flixclusive.feature.mobile.library.manage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.collections.SortUtils
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.presentation.common.components.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.ViewModelUtil.activityHiltViewModel
import com.flixclusive.core.presentation.mobile.components.CommonPullToRefreshBox
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.material3.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState
import com.flixclusive.feature.mobile.library.common.component.CreateLibraryDialog
import com.flixclusive.feature.mobile.library.common.component.EditLibraryDialog
import com.flixclusive.feature.mobile.library.common.component.LibraryFilterRow
import com.flixclusive.feature.mobile.library.common.model.TrackerProvider
import com.flixclusive.feature.mobile.library.common.util.selectionBorder
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.component.DefaultLibraryCardShape
import com.flixclusive.feature.mobile.library.manage.component.LibraryCard
import com.flixclusive.feature.mobile.library.manage.component.LibraryCardPlaceholder
import com.flixclusive.feature.mobile.library.manage.component.LibraryOptionsBottomSheet
import com.flixclusive.feature.mobile.library.manage.component.ManageLibraryTopBar
import com.flixclusive.feature.mobile.library.manage.component.TrackerProvidersBottomSheet
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>
@Composable
internal fun ManageLibraryScreen(
    navigator: NavigatorManageLibraryScreen,
    viewModel: ManageLibraryViewModel = activityHiltViewModel(),
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()

    ManageLibraryScreenContent(
        uiState = uiState,
        searchQuery = { searchQuery },
        selectedLists = { viewModel.selectedLists },
        lists = { lists },
        trackers = { trackers },
        onToggleTracker = viewModel::onToggleTracker,
        onRefresh = { viewModel.initialize(isRefreshing = true) },
        onConsumeTrackerErrors = viewModel::onConsumeTrackerErrors,
        onRemoveLongClickedLibrary = viewModel::onRemoveLongClickedLibrary,
        onLongClickItem = viewModel::onLongClickItem,
        onStartMultiSelecting = viewModel::onStartMultiSelecting,
        onToggleSelect = viewModel::onToggleSelect,
        onUpdateFilter = viewModel::onUpdateFilter,
        onRemoveSelection = viewModel::onRemoveSelection,
        onQueryChange = viewModel::onQueryChange,
        onUnselectAll = viewModel::onUnselectAll,
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onToggleOptionsSheet = viewModel::onToggleOptionsSheet,
        onToggleEditDialog = viewModel::onToggleEditDialog,
        onToggleCreateDialog = viewModel::onToggleCreateDialog,
        onSaveEdits = viewModel::onSaveEdits,
        onCreate = viewModel::onAdd,
        onViewLibraryContent = { navigator.navigateToLibraryDetailsScreen(it.list, it.provider) },
        openProviderSettings = {
            if (!it.isAuthenticated) {
                viewModel.onTrackerSignIn(it)
            }

            navigator.navigateToProviderSettings(it.metadata)
        },
    )
}

@OptIn(FlowPreview::class)
@Composable
private fun ManageLibraryScreenContent(
    uiState: ManageLibraryUiState,
    lists: () -> Async<List<LibraryListWithPreview>>,
    trackers: () -> Async<List<TrackerProvider>>,
    selectedLists: () -> Set<LibraryListWithPreview>,
    searchQuery: () -> String,
    onRefresh: () -> Unit,
    onConsumeTrackerErrors: () -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onSaveEdits: (LibraryListWithPreview) -> Unit,
    onCreate: (String, String?, TrackerProvider?) -> Unit,
    onToggleEditDialog: (Boolean) -> Unit,
    onToggleCreateDialog: (Boolean) -> Unit,
    onRemoveLongClickedLibrary: () -> Unit,
    onViewLibraryContent: (LibraryListWithPreview) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (LibraryListWithPreview) -> Unit,
    onToggleOptionsSheet: (Boolean) -> Unit,
    onLongClickItem: (LibraryListWithPreview) -> Unit,
    onUpdateFilter: (LibrarySort) -> Unit,
    openProviderSettings: (TrackerProvider) -> Unit,
    onToggleTracker: (TrackerProvider) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    val refreshState = rememberPullToRefreshState()
    var isFabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollBehavior.state.heightOffset }
            .debounce(800)
            .distinctUntilChanged()
            .collect {
                isFabExpanded = it < 0f
            }
    }

    val listState = rememberLazyGridState()

    val selectCount by remember {
        derivedStateOf { selectedLists().size }
    }

    val isListEmpty by remember {
        derivedStateOf {
            (lists() as? Async.Success)?.data?.isEmpty() == true
        }
    }

    val isFabVisible by remember {
        derivedStateOf { !uiState.isMultiSelecting && lists() is Async.Success }
    }

    var showDeleteLibraryAlert by remember { mutableStateOf(false) }
    var showDeleteSelectionAlert by remember { mutableStateOf(false) }
    var showTrackerOptions by remember { mutableStateOf(false) }

    CommonPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        state = refreshState,
        onRefresh = {
            val isLoadingAlready = trackers() is Async.Loading && uiState.isLoadingTrackers
            if (!uiState.isRefreshing && !isLoadingAlready) {
                onRefresh()
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(LocalGlobalScaffoldPadding.current),
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {
                AnimatedVisibility(visible = isFabVisible) {
                    ExtendedFloatingActionButton(
                        onClick = { onToggleCreateDialog(true) },
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                        shape = MaterialTheme.shapes.medium,
                        expanded = isFabExpanded,
                        text = { Text(text = stringResource(LocaleR.string.new_list)) },
                        icon = {
                            Icon(
                                painter = painterResource(id = UiCommonR.drawable.round_add_24),
                                contentDescription = stringResource(LocaleR.string.plus_button_content_desc),
                            )
                        },
                    )
                }
            },
            topBar = {
                val topBarState = remember(
                    uiState.isMultiSelecting,
                    uiState.isShowingSearchBar,
                ) {
                    if (uiState.isMultiSelecting) {
                        LibraryTopBarState.Selecting
                    } else if (uiState.isShowingSearchBar) {
                        LibraryTopBarState.Searching
                    } else {
                        LibraryTopBarState.DefaultMainScreen
                    }
                }

                ManageLibraryTopBar(
                    topBarState = topBarState,
                    isListEmpty = isListEmpty,
                    selectCount = { selectCount },
                    enableTrackerButton = { trackers().let { it is Async.Success && it.data.isNotEmpty() } },
                    scrollBehavior = scrollBehavior,
                    searchQuery = searchQuery,
                    onShowTrackers = { showTrackerOptions = true },
                    onToggleSearchBar = onToggleSearchBar,
                    onQueryChange = onQueryChange,
                    onUnselectAll = onUnselectAll,
                    onRemoveSelection = { showDeleteSelectionAlert = true },
                    title = {
                        val title =
                            if (topBarState == LibraryTopBarState.Selecting) {
                                stringResource(LocaleR.string.count_selection_format, selectCount)
                            } else {
                                stringResource(LocaleR.string.my_library)
                            }

                        Text(
                            text = title,
                            style = getTopBarHeadlinerTextStyle(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                ) {
                    LibraryFilterRow(
                        isListEditable = !isListEmpty && !uiState.isMultiSelecting,
                        selected = { uiState.selectedFilter },
                        onUpdate = onUpdateFilter,
                        onStartSelecting = onStartMultiSelecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp),
                    )
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = isListEmpty,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyDataMessage(modifier = Modifier.padding(padding))
                } else {
                    ManageLibraryScreenContent(
                        data = lists,
                        uiState = uiState,
                        listState = listState,
                        padding = padding,
                        selectedLists = selectedLists,
                        onToggleSelect = onToggleSelect,
                        onViewLibraryContent = onViewLibraryContent,
                        onLongClickItem = onLongClickItem,
                        onToggleOptionsSheet = onToggleOptionsSheet,
                    )
                }
            }
        }
    }

    if (uiState.isShowingOptionsSheet) {
        LibraryOptionsBottomSheet(
            onEdit = { onToggleEditDialog(true) },
            onDelete = {
                showDeleteLibraryAlert = true
                onToggleOptionsSheet(false)
            },
            onDismissRequest = { onToggleOptionsSheet(false) },
        )
    }

    if (uiState.isEditingLibrary && uiState.longClickedLibrary != null) {
        EditLibraryDialog(
            library = uiState.longClickedLibrary.list,
            onSave = {
                onSaveEdits(
                    uiState.longClickedLibrary.copy(list = it)
                )
            },
            onCancel = { onToggleEditDialog(false) },
        )
    }

    if (uiState.isCreatingLibrary) {
        CreateLibraryDialog(
            trackers = trackers,
            onCreate = { name, desc, tracker ->
                onCreate(name, desc, tracker)
                onToggleCreateDialog(false)
            },
            onCancel = { onToggleCreateDialog(false) },
        )
    }

    if (showTrackerOptions) {
        TrackerProvidersBottomSheet(
            trackers = trackers,
            openProviderSettings = {
                openProviderSettings(it)
                showTrackerOptions = false
            },
            onDismiss = { showTrackerOptions = false },
            onToggle = onToggleTracker
        )
    }
    if (uiState.trackerErrors.isNotEmpty()) {
        ProviderCrashBottomSheet(
            isLoading = false,
            errors = uiState.trackerErrors,
            onDismissRequest = onConsumeTrackerErrors,
        )
    }

    if (showDeleteLibraryAlert || showDeleteSelectionAlert) {
        val alertDescription =
            if (showDeleteLibraryAlert) {
                val libraryName = uiState.longClickedLibrary?.list?.name ?: ""
                stringResource(LocaleR.string.warn_delete_library_format, libraryName)
            } else {
                stringResource(LocaleR.string.warn_delete_selected_libraries_format)
            }

        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            description = alertDescription,
            onConfirm = {
                if (showDeleteLibraryAlert) {
                    onRemoveLongClickedLibrary()
                } else {
                    onRemoveSelection()
                }
            },
            onDismiss = {
                showDeleteLibraryAlert = false
                showDeleteSelectionAlert = false
            },
        )
    }
}

@Composable
private fun ManageLibraryScreenContent(
    data: () -> Async<List<LibraryListWithPreview>>,
    listState: LazyGridState,
    padding: PaddingValues,
    uiState: ManageLibraryUiState,
    selectedLists: () -> Set<LibraryListWithPreview>,
    onToggleSelect: (LibraryListWithPreview) -> Unit,
    onViewLibraryContent: (LibraryListWithPreview) -> Unit,
    onLongClickItem: (LibraryListWithPreview) -> Unit,
    onToggleOptionsSheet: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val lists by remember(data) {
        derivedStateOf {
            (data() as? Async.Success)?.data ?: emptyList()
        }
    }

    val isLoading by remember(uiState) {
        derivedStateOf {
            data() is Async.Loading || uiState.isLoadingTrackers
        }
    }

    LazyVerticalGrid(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = padding,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(
            getAdaptiveDp(
                compact = 300.dp,
                medium = 350.dp,
                expanded = 400.dp,
            ),
        ),
    ) {
        Snapshot.withoutReadObservation {
            listState.requestScrollToItem(
                index = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
            )
        }

        items(
            items = lists,
            key = { it.hashCode() },
        ) { library ->
            val selected by remember {
                derivedStateOf { selectedLists().contains(library) }
            }

            LibraryCard(
                libraryListWithPreview = library,
                onClick = {
                    if (uiState.isMultiSelecting) {
                        if (!library.list.isCustom) {
                            context.showToast(
                                resources.getString(R.string.failed_to_select_system_list_message)
                            )
                            return@LibraryCard
                        }
                        onToggleSelect(library)
                    } else {
                        onViewLibraryContent(library)
                    }
                },
                onLongClick = {
                    if (library.list.isCustom) {
                        onLongClickItem(library)
                        onToggleOptionsSheet(true)
                    }
                },
                modifier = Modifier
                    .animateItem()
                    .selectionBorder(
                        isSelected = selected,
                        shape = DefaultLibraryCardShape,
                    ),
            )
        }

        if (isLoading) {
            items(count = 4, key = { "tracker_placeholder_$it" }) {
                LibraryCardPlaceholder(
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManageLibraryScreenBasePreview() {
    var searchQuery by remember { mutableStateOf("") }
    var uiState by remember { mutableStateOf(ManageLibraryUiState()) }
    val libraries = remember { mutableStateListOf<LibraryListWithPreview>() }
    var selectedLibraries by remember { mutableStateOf(persistentSetOf<LibraryListWithPreview>()) }

    val safeLibraries: Async<List<LibraryListWithPreview>> by remember {
        derivedStateOf {
            val list =
                if (searchQuery.isNotEmpty()) {
                    libraries.fastFilter {
                        it.list.name.contains(searchQuery, true) ||
                            it.list.description?.contains(searchQuery, true) == true
                    }
                } else {
                    libraries
                }

            val sortedList =
                list.sortedWith(
                    SortUtils.compareBy(
                        ascending = uiState.selectedFilter.ascending,
                        selector = {
                            when (uiState.selectedFilter) {
                                is LibrarySort.Name -> it.list.name
                                is LibrarySort.Added -> it.list.createdAt.time
                                is LibrarySort.Modified -> it.list.updatedAt.time
                            }
                        },
                    ),
                )

            Async.Success(sortedList)
        }
    }

    LaunchedEffect(true) {
        libraries.addAll(
            List(10) { i ->
                val previews = List(3) { j ->
                    DummyDataForPreview
                        .getMovie(
                            title = "MediaMetadata #$j",
                            id = j.toString(),
                        ).toPreviewPoster()
                }
                val description =
                    if (Random.nextBoolean()) {
                        "Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum"
                    } else {
                        null
                    }

                val list = LibraryList(
                    id = i.toString(),
                    ownerId = "preview-user",
                    name = "Library $i",
                    description = description,
                )

                LibraryListWithPreview(
                    list = list,
                    itemsCount = Random.nextInt(1, 500),
                    previews = previews,
                    provider = DummyDataForPreview.getProviderMetadata()
                )
            },
        )
        delay(3000)
    }

    FlixclusiveTheme {
        Surface {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ) {
                ManageLibraryScreenContent(
                    uiState = uiState,
                    lists = { safeLibraries },
                    trackers = { Async.Success(emptyList()) },
                    selectedLists = { selectedLibraries },
                    searchQuery = { searchQuery },
                    onRefresh = {},
                    onRemoveSelection = { libraries.removeAll(selectedLibraries) },
                    onStartMultiSelecting = { uiState = uiState.copy(isMultiSelecting = true) },
                    onUnselectAll = {
                        selectedLibraries = persistentSetOf()
                        uiState = uiState.copy(isMultiSelecting = false)
                    },
                    onSaveEdits = {
                        val index = libraries.indexOf(uiState.longClickedLibrary)
                        libraries[index] = it
                        uiState = uiState.copy(
                            isEditingLibrary = false,
                            longClickedLibrary = null,
                        )
                    },
                    onCreate = { _, _, _ -> },
                    onToggleEditDialog = {
                        uiState =
                            uiState.copy(
                                isShowingOptionsSheet = false,
                                isEditingLibrary = true,
                            )
                    },
                    onToggleCreateDialog = {},
                    onRemoveLongClickedLibrary = {
                        uiState =
                            with(uiState) {
                                libraries.remove(longClickedLibrary)
                                copy(
                                    isShowingOptionsSheet = false,
                                    longClickedLibrary = null,
                                )
                            }
                    },
                    onViewLibraryContent = { },
                    onQueryChange = { searchQuery = it },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                    onToggleSelect = {
                        selectedLibraries = with(selectedLibraries) {
                            if (contains(it)) remove(it) else add(it)
                        }
                    },
                    onToggleOptionsSheet = { uiState = uiState.copy(isShowingOptionsSheet = it) },
                    onLongClickItem = { uiState = uiState.copy(longClickedLibrary = it) },
                    openProviderSettings = { },
                    onToggleTracker = { },
                    onConsumeTrackerErrors = {},
                    onUpdateFilter = {
                        if (uiState.selectedFilter == it) {
                            uiState.selectedFilter.toggleAscending()
                            return@ManageLibraryScreenContent
                        }

                        uiState = uiState.copy(selectedFilter = it)
                    },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ManageLibraryScreenCompactLandscapePreview() {
    ManageLibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ManageLibraryScreenMediumPortraitPreview() {
    ManageLibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ManageLibraryScreenMediumLandscapePreview() {
    ManageLibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ManageLibraryScreenExtendedPortraitPreview() {
    ManageLibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ManageLibraryScreenExtendedLandscapePreview() {
    ManageLibraryScreenBasePreview()
}
