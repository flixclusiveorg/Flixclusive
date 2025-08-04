package com.flixclusive.feature.mobile.library.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.ui.mobile.component.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.ui.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState
import com.flixclusive.feature.mobile.library.common.component.LibraryFilterRow
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.common.util.selectionBorder
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.manage.component.CreateLibraryDialog
import com.flixclusive.feature.mobile.library.manage.component.DefaultLibraryCardShape
import com.flixclusive.feature.mobile.library.manage.component.EditLibraryDialog
import com.flixclusive.feature.mobile.library.manage.component.LibraryCard
import com.flixclusive.feature.mobile.library.manage.component.LibraryOptionsBottomSheet
import com.flixclusive.feature.mobile.library.manage.component.topbar.ManageLibraryTopBar
import com.flixclusive.feature.mobile.library.manage.util.mapToListPreview
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
        onToggleSearchBar = viewModel::onToggleSearchBar,
        onToggleOptionsSheet = viewModel::onToggleOptionsSheet,
        onToggleEditDialog = viewModel::onToggleEditDialog,
        onToggleCreateDialog = viewModel::onToggleCreateDialog,
        onSaveEdits = viewModel::onSaveEdits,
        onCreate = viewModel::onAdd,
    )
}

@Composable
private fun ManageLibraryScreen(
    uiState: () -> ManageLibraryUiState,
    libraries: () -> List<UiLibraryList>,
    selectedLibraries: () -> Set<LibraryListWithPreview>,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onSaveEdits: (LibraryList) -> Unit,
    onCreate: (String, String?) -> Unit,
    onToggleEditDialog: (Boolean) -> Unit,
    onToggleCreateDialog: (Boolean) -> Unit,
    onRemoveLongClickedLibrary: () -> Unit,
    onViewLibraryContent: (LibraryList) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (LibraryListWithPreview) -> Unit,
    onToggleOptionsSheet: (Boolean) -> Unit,
    onLongClickItem: (LibraryListWithPreview) -> Unit,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    var isFabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(scrollBehavior.state.heightOffset) {
        delay(800)
        isFabExpanded = scrollBehavior.state.heightOffset < 0f
    }

    val listState = rememberLazyGridState()

    val selectCount by remember {
        derivedStateOf { selectedLibraries().size }
    }

    val searchQuery by remember {
        derivedStateOf { uiState().searchQuery }
    }

    var showDeleteLibraryAlert by remember { mutableStateOf(false) }
    var showDeleteSelectionAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(LocalGlobalScaffoldPadding.current),
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onToggleCreateDialog(true) },
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                shape = MaterialTheme.shapes.medium,
                expanded = isFabExpanded,
                text = {
                    Text(text = stringResource(LocaleR.string.create_a_library))
                },
                icon = {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.round_add_24),
                        contentDescription = stringResource(LocaleR.string.plus_button_content_desc),
                    )
                },
            )
        },
        topBar = {
            val topBarState by remember {
                derivedStateOf {
                    if (uiState().isMultiSelecting) {
                        LibraryTopBarState.Selecting
                    } else if (uiState().isShowingSearchBar) {
                        LibraryTopBarState.Searching
                    } else {
                        LibraryTopBarState.DefaultMainScreen
                    }
                }
            }

            ManageLibraryTopBar(
                topBarState = topBarState,
                isListEmpty = libraries().isEmpty(),
                selectCount = { selectCount },
                scrollBehavior = scrollBehavior,
                searchQuery = { searchQuery },
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onRemoveSelection = { showDeleteSelectionAlert = true },
                onUnselectAll = onUnselectAll,
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
                    isListEditable = libraries().isNotEmpty() && !uiState().isMultiSelecting,
                    filters = defaultManageLibraryFilters,
                    currentFilter = uiState().selectedFilter,
                    currentDirection = uiState().selectedFilterDirection,
                    onUpdateFilter = onUpdateFilter,
                    onStartSelecting = onStartMultiSelecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp),
                )
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            columns =
                GridCells.Adaptive(
                    getAdaptiveDp(
                        compact = 300.dp,
                        medium = 350.dp,
                        expanded = 400.dp,
                    ),
                ),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
        ) {
            Snapshot.withoutReadObservation {
                listState.requestScrollToItem(
                    index = listState.firstVisibleItemIndex,
                    scrollOffset = listState.firstVisibleItemScrollOffset,
                )
            }

            items(
                items = libraries(),
                key = {
                    val preview = it.mapToListPreview()
                    preview.list.id
                },
            ) { library ->
                val item = library.mapToListPreview()
                val name =
                    if (library is EmphasisLibraryList) {
                        library.name
                    } else {
                        UiText.from((library as LibraryListWithPreview).list.name)
                    }

                val description =
                    if (library is EmphasisLibraryList) {
                        library.description
                    } else {
                        (library as LibraryListWithPreview).list.description?.let(UiText::from)
                    }

                LibraryCard(
                    name = name,
                    description = description,
                    itemsCount = item.itemsCount,
                    previews = item.previews,
                    onClick = {
                        when (library) {
                            is LibraryListWithPreview -> {
                                if (uiState().isMultiSelecting) {
                                    onToggleSelect(library)
                                } else {
                                    onViewLibraryContent(library.list)
                                }
                            }

                            is EmphasisLibraryList -> {
                                onViewLibraryContent(library.library.list)
                            }
                        }
                    },
                    onLongClick =
                        if (library is LibraryListWithPreview) {
                            fun() {
                                onLongClickItem(library)
                                onToggleOptionsSheet(true)
                            }
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .animateItem()
                            .selectionBorder(
                                isSelected = selectedLibraries().contains(library),
                                shape = DefaultLibraryCardShape,
                            ),
                )
            }
        }
    }

    if (uiState().isShowingOptionsSheet) {
        LibraryOptionsBottomSheet(
            onEdit = { onToggleEditDialog(true) },
            onDelete = {
                showDeleteLibraryAlert = true
                onToggleOptionsSheet(false)
            },
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

    if (uiState().isCreatingLibrary) {
        CreateLibraryDialog(
            onCreate = onCreate,
            onCancel = { onToggleCreateDialog(false) },
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

@Preview
@Composable
private fun ManageLibraryScreenBasePreview() {
    var uiState by remember { mutableStateOf(ManageLibraryUiState()) }
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
                                ItemCount -> it.itemsCount
                                else -> throw Error()
                            }
                        },
                    ).let { comparator ->
                        if (uiState.selectedFilterDirection == LibraryFilterDirection.ASC) {
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
                    onCreate = { _, _ -> },
                    onToggleCreateDialog = {},
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
