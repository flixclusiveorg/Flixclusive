package com.flixclusive.feature.mobile.library.manage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.EmptyDataMessage
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.topbar.ActionButton
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.core.ui.mobile.component.topbar.DefaultNavigationIcon
import com.flixclusive.core.ui.mobile.component.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.component.DefaultLibraryCardShape
import com.flixclusive.feature.mobile.library.manage.component.EditLibraryDialog
import com.flixclusive.feature.mobile.library.manage.component.LibraryCard
import com.flixclusive.feature.mobile.library.manage.component.LibraryFilterBottomSheet
import com.flixclusive.feature.mobile.library.manage.component.LibraryOptionsBottomSheet
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface ManageLibraryScreenNavigator : GoBackAction {
    fun openLibraryDetails(list: LibraryList)
}

@Destination
@Composable
internal fun ManageLibraryScreen(
    navigator: ManageLibraryScreenNavigator,
    viewModel: ManageLibraryViewModel = hiltViewModel(),
) {
    val libraries = viewModel.libraries.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLibraries = viewModel.selectedLibraries.collectAsStateWithLifecycle(emptySet())

    ManageLibraryScreen(
        uiState = { uiState.value },
        libraries = { libraries.value },
        selectedLibraries = { selectedLibraries.value },
        onRemoveLongClickedLibrary = viewModel::onRemoveLongClickedLibrary,
        onViewLibraryContent = navigator::openLibraryDetails,
        onLongClickItem = viewModel::onLongClickItem,
        onStartMultiSelecting = viewModel::onStartMultiSelecting,
        onToggleSelect = viewModel::onToggleSelect,
        onUpdateFilter = viewModel::onUpdateFilter,
        onRemoveSelection = viewModel::onRemoveSelection,
        onQueryChange = viewModel::onQueryChange,
        onUnselectAll = viewModel::onUnselectAll,
        onToggleFilterSheet = viewModel::onToggleFilterSheet,
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onToggleOptionsSheet = viewModel::onToggleOptionsSheet,
        onToggleEditDialog = viewModel::onToggleEditDialog,
        onSaveEdits = viewModel::onSaveEdits,
    )
}

@Composable
private fun ManageLibraryScreen(
    uiState: () -> LibraryUiState,
    libraries: () -> List<LibraryListWithPreview>,
    selectedLibraries: () -> Set<LibraryListWithPreview>,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onSaveEdits: (LibraryList) -> Unit,
    onToggleEditDialog: (Boolean) -> Unit,
    onRemoveLongClickedLibrary: () -> Unit,
    onViewLibraryContent: (LibraryList) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleFilterSheet: (Boolean) -> Unit,
    onToggleSelect: (LibraryListWithPreview) -> Unit,
    onToggleOptionsSheet: (Boolean) -> Unit,
    onLongClickItem: (LibraryListWithPreview) -> Unit,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    val listState = rememberLazyListState()

    val selectedColor = MaterialTheme.colorScheme.tertiary
    val selectCount by remember {
        derivedStateOf { selectedLibraries().size }
    }

    val searchQuery by remember {
        derivedStateOf { uiState().searchQuery }
    }

    var showDeleteLibraryAlert by remember { mutableStateOf(false) }
    var showDeleteSelectionAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val navigationState by remember {
                derivedStateOf {
                    if (uiState().isMultiSelecting) {
                        TopBarNavigationState.Selecting
                    } else if (uiState().isShowingSearchBar) {
                        TopBarNavigationState.Searching
                    } else {
                        TopBarNavigationState.Default
                    }
                }
            }

            ManageLibraryTopBar(
                navigationState = navigationState,
                isListEmpty = libraries().isEmpty(),
                selectCount = { selectCount },
                scrollBehavior = scrollBehavior,
                searchQuery = { searchQuery },
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onShowFilterSheet = { onToggleFilterSheet(true) },
                onRemoveSelection = { showDeleteSelectionAlert = true },
                onStartMultiSelecting = onStartMultiSelecting,
                onUnselectAll = onUnselectAll,
            )
        },
    ) {
        val padding by remember {
            derivedStateOf { it }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = padding,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Snapshot.withoutReadObservation {
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                }

                items(
                    items = libraries(),
                    key = { it.list.id },
                ) { library ->
                    LibraryCard(
                        library = library,
                        onClick = {
                            if (uiState().isMultiSelecting) {
                                onToggleSelect(library)
                            } else {
                                onViewLibraryContent(library.list)
                            }
                        },
                        onLongClick = {
                            onLongClickItem(library)
                            onToggleOptionsSheet(true)
                        },
                        modifier =
                            Modifier
                                .animateItem()
                                .border(
                                    shape = DefaultLibraryCardShape,
                                    border =
                                        BorderStroke(
                                            width = Dp.Hairline,
                                            color =
                                                if (selectedLibraries().contains(library)) {
                                                    selectedColor
                                                } else {
                                                    Color.Transparent
                                                },
                                        ),
                                ),
                    )
                }
            }

            AnimatedVisibility(
                visible = libraries().isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                EmptyDataMessage(
                    modifier =
                        Modifier
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    }

    if (uiState().isShowingFilterSheet) {
        LibraryFilterBottomSheet(
            currentFilter = uiState().selectedFilter,
            currentDirection = uiState().selectedFilterDirection,
            onDismissRequest = { onToggleFilterSheet(false) },
            onUpdateFilter = onUpdateFilter,
        )
    }

    if (uiState().isShowingOptionsSheet) {
        LibraryOptionsBottomSheet(
            onEdit = { onToggleEditDialog(true) },
            onDelete = { showDeleteLibraryAlert = true },
            onDismissRequest = { onToggleOptionsSheet(false) },
        )
    }

    if (uiState().isEditingLibrary && uiState().longClickedLibrary != null) {
        EditLibraryDialog(
            library = uiState().longClickedLibrary!!.list,
            onSave = onSaveEdits,
            onCancel = { onToggleEditDialog(false) },
        )
    }

    if (showDeleteLibraryAlert || showDeleteSelectionAlert) {
        val alertDescription =
            if (showDeleteLibraryAlert) {
                val libraryName = uiState().longClickedLibrary?.list?.name ?: ""
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
                    showDeleteLibraryAlert = false
                } else {
                    onRemoveSelection()
                    showDeleteSelectionAlert = false
                }
            },
        )
    }
}

private enum class TopBarNavigationState {
    Default,
    Selecting,
    Searching,
}

@Composable
private fun ManageLibraryTopBar(
    navigationState: TopBarNavigationState,
    scrollBehavior: TopAppBarScrollBehavior,
    isListEmpty: Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onUnselectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title =
        if (navigationState == TopBarNavigationState.Selecting) {
            context.getString(LocaleR.string.count_selection_format, selectCount())
        } else {
            context.getString(LocaleR.string.my_library)
        }

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = navigationState == TopBarNavigationState.Searching,
        title = title,
        onNavigate = {},
        navigationIcon = {
            AnimatedContent(
                targetState = navigationState,
            ) { state ->
                if (state == TopBarNavigationState.Selecting) {
                    PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                        ActionButton(onClick = onUnselectAll) {
                            AdaptiveIcon(
                                painter = painterResource(UiCommonR.drawable.round_close_24),
                                contentDescription = stringResource(LocaleR.string.cancel),
                            )
                        }
                    }
                } else if (state == TopBarNavigationState.Searching) {
                    DefaultNavigationIcon(
                        onClick = { onToggleSearchBar(false) },
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        hideSearchButton = navigationState != TopBarNavigationState.Default || isListEmpty,
        extraActions = {
            AnimatedContent(
                targetState = navigationState,
            ) { state ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state == TopBarNavigationState.Selecting && !isListEmpty) {
                        PlainTooltipBox(description = stringResource(LocaleR.string.remove)) {
                            ActionButton(
                                onClick = onRemoveSelection,
                                enabled = selectCount() > 0,
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.delete),
                                    contentDescription = stringResource(LocaleR.string.remove),
                                    dp = 24.dp,
                                )
                            }
                        }
                    } else if (!isListEmpty) {
                        PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                            ActionButton(onClick = onShowFilterSheet) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.filter_list),
                                    contentDescription = stringResource(LocaleR.string.filter_button),
                                    dp = 24.dp,
                                )
                            }
                        }

                        PlainTooltipBox(description = stringResource(LocaleR.string.multi_select)) {
                            ActionButton(onClick = onStartMultiSelecting) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.select),
                                    contentDescription = stringResource(LocaleR.string.multi_select),
                                    tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                                    dp = 24.dp,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun ManageLibraryScreenBasePreview() {
    var uiState by remember { mutableStateOf(LibraryUiState()) }
    val libraries = remember { mutableStateListOf<LibraryListWithPreview>() }

    val safeLibraries by remember {
        derivedStateOf {
            val query = uiState.searchQuery
            val list =
                if (query.isNotEmpty()) {
                    libraries.filter {
                        it.list.name.contains(query, true) || it.list.description?.contains(query, true) == true
                    }
                } else {
                    libraries
                }

            val sortedList =
                list.sortedWith(
                    compareBy<LibraryListWithPreview>(
                        selector = {
                            when (uiState.selectedFilter) {
                                LibrarySortFilter.Name -> it.list.name
                                LibrarySortFilter.AddedAt -> it.list.createdAt.time
                                LibrarySortFilter.ModifiedAt -> it.list.updatedAt.time
                                LibrarySortFilter.ItemCount -> it.itemsCount
                            }
                        },
                    ).let { comparator ->
                        if (uiState.selectedFilterDirection == LibrarySortFilter.Direction.ASC) {
                            comparator
                        } else {
                            comparator.reversed()
                        }
                    },
                )

            sortedList
        }
    }

    LaunchedEffect(true) {
        libraries.addAll(
            List(10) {
                val previews = List(3) { DBFilm(title = "Film #$it").toPreviewPoster() }
                val description =
                    if (Random.nextBoolean()) {
                        "Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum"
                    } else {
                        null
                    }
                val list =
                    LibraryList(
                        id = it,
                        ownerId = 1,
                        name = "Library $it",
                        description = description,
                    )

                LibraryListWithPreview(
                    list = list,
                    itemsCount = Random.nextInt(1, 500),
                    previews = previews,
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
                ManageLibraryScreen(
                    uiState = { uiState },
                    libraries = { safeLibraries },
                    selectedLibraries = { uiState.selectedLibraries },
                    onUpdateFilter = {
                        uiState =
                            if (uiState.selectedFilter == it) {
                                uiState.copy(selectedFilterDirection = uiState.selectedFilterDirection.toggle())
                            } else {
                                uiState.copy(selectedFilter = it)
                            }
                    },
                    onViewLibraryContent = {},
                    onStartMultiSelecting = { uiState = uiState.copy(isMultiSelecting = true) },
                    onRemoveSelection = { libraries.removeAll(uiState.selectedLibraries) },
                    onToggleSelect = {
                        uiState =
                            with(uiState) {
                                val newList = selectedLibraries.toMutableSet()
                                newList.add(it)
                                copy(selectedLibraries = newList.toSet())
                            }
                    },
                    onUnselectAll = {
                        uiState =
                            uiState.copy(
                                selectedLibraries = emptySet(),
                                isMultiSelecting = false,
                            )
                    },
                    onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                    onToggleFilterSheet = { uiState = uiState.copy(isShowingFilterSheet = it) },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                    onToggleOptionsSheet = { uiState = uiState.copy(isShowingOptionsSheet = it) },
                    onLongClickItem = { uiState = uiState.copy(longClickedLibrary = it) },
                    onToggleEditDialog = {
                        uiState =
                            uiState.copy(
                                isShowingOptionsSheet = false,
                                isEditingLibrary = true,
                            )
                    },
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
                    onSaveEdits = {
                        val index = libraries.indexOf(uiState.longClickedLibrary)
                        val libraryWithPreview = libraries[index]
                        libraries[index] = libraryWithPreview.copy(list = it)
                        uiState =
                            uiState.copy(
                                isEditingLibrary = false,
                                longClickedLibrary = null,
                            )
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
