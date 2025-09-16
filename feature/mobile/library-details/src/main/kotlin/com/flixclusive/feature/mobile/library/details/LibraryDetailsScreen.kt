package com.flixclusive.feature.mobile.library.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.presentation.common.components.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.LoadingScreen
import com.flixclusive.core.presentation.mobile.components.dialog.IconAlertDialog
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.components.topbar.rememberEnterOnlyNearTopScrollBehavior
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.feature.mobile.library.common.LibraryTopBarState
import com.flixclusive.feature.mobile.library.common.component.LibraryFilterRow
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.common.util.selectionBorder
import com.flixclusive.feature.mobile.library.details.component.ScreenHeader
import com.flixclusive.feature.mobile.library.details.component.topbar.LibraryDetailsTopBar
import com.flixclusive.feature.mobile.library.details.component.topbar.TopTitleAlphaEasing
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import java.util.Date
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Destination(navArgsDelegate = LibraryDetailsNavArgs::class)
@Composable
internal fun LibraryDetailsScreen(
    navigator: LibraryDetailsScreenNavigator,
    viewModel: LibraryDetailsViewModel = hiltViewModel(),
) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val searchItems by viewModel.searchItems.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    LibraryDetailsScreen(
        library = library,
        uiState = uiState,
        items = {
            if (searchQuery.isNotEmpty()
                && uiState.isShowingSearchBar) {
                searchItems
            } else items
        },
        searchQuery = { searchQuery },
        selectedItems = { selectedItems },
        onGoBack = navigator::goBack,
        onViewFilm = navigator::openFilmScreen,
        onAddItems = { /*TODO()*/ },
        onRemoveLongClickedItem = viewModel::onRemoveLongClickedItem,
        onLongClickItem = viewModel::onLongClickItem,
        onStartMultiSelecting = viewModel::onStartMultiSelecting,
        onToggleSelect = viewModel::onToggleSelect,
        onUpdateFilter = viewModel::onUpdateFilter,
        onRemoveSelection = viewModel::onRemoveSelection,
        onQueryChange = viewModel::onQueryChange,
        onUnselectAll = viewModel::onUnselectAll,
        onToggleSearchBar = viewModel::onToggleSearchBar,
    )
}

@Composable
internal fun LibraryDetailsScreen(
    library: LibraryList,
    uiState: LibraryDetailsUiState,
    searchQuery: () -> String,
    items: () -> PersistentList<LibraryListItemWithMetadata>,
    selectedItems: () -> PersistentSet<LibraryListItemWithMetadata>,
    onGoBack: () -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onAddItems: () -> Unit,
    onRemoveLongClickedItem: () -> Unit,
    onViewFilm: (Film) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleSelect: (LibraryListItemWithMetadata) -> Unit,
    onLongClickItem: (LibraryListItemWithMetadata) -> Unit,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
) {
    val scrollBehavior = rememberEnterOnlyNearTopScrollBehavior()

    val listState = rememberLazyGridState()

    val selectCount by remember {
        derivedStateOf { selectedItems().size }
    }

    var showDeleteItemAlert by remember { mutableStateOf(false) }
    var showDeleteSelectionAlert by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val topBarState by remember {
                derivedStateOf {
                    if (uiState.isMultiSelecting) {
                        LibraryTopBarState.Selecting
                    } else if (uiState.isShowingSearchBar) {
                        LibraryTopBarState.Searching
                    } else {
                        LibraryTopBarState.DefaultSubScreen
                    }
                }
            }

            LibraryDetailsTopBar(
                topBarState = topBarState,
                scrollBehavior = scrollBehavior,
                isListEmpty = items().isEmpty(),
                onGoBack = onGoBack,
                selectCount = selectCount,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onRemoveSelection = { showDeleteSelectionAlert = true },
                onUnselectAll = onUnselectAll,
                title = {
                    val title =
                        if (topBarState == LibraryTopBarState.Selecting) {
                            stringResource(LocaleR.string.count_selection_format, selectCount)
                        } else {
                            library.name
                        }

                    Text(
                        text = title,
                        style = getTopBarHeadlinerTextStyle(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier =
                            Modifier.graphicsLayer {
                                alpha =
                                    when (topBarState) {
                                        LibraryTopBarState.Selecting -> 1f
                                        else -> TopTitleAlphaEasing.transform(scrollBehavior.state.collapsedFraction)
                                    }
                            },
                    )
                },
                infoContent = {
                    ScreenHeader(
                        library = library,
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
                            isListEditable = items().isNotEmpty() && !uiState.isMultiSelecting,
                            filters = LibraryDetailsFilters.defaultFilters,
                            selected = uiState.selectedFilter,
                            ascending = uiState.isSortingAscending,
                            onUpdate = onUpdateFilter,
                            onStartSelecting = onStartMultiSelecting,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AnimatedContent(
            uiState.isLoading,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                true -> {
                    LoadingScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                false -> {
                    LazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Adaptive(getAdaptiveFilmCardWidth()),
                        contentPadding = paddingValues,
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Snapshot.withoutReadObservation {
                            listState.requestScrollToItem(
                                index = listState.firstVisibleItemIndex,
                                scrollOffset = listState.firstVisibleItemScrollOffset,
                            )
                        }

                        items(items = items(), key = { it.itemId }) { item ->
                            val film = item.metadata
                            val isSelected by remember {
                                derivedStateOf { selectedItems().contains(item) }
                            }

                            FilmCard(
                                film = film,
                                onClick = {
                                    if (uiState.isMultiSelecting) {
                                        onToggleSelect(item)
                                    } else {
                                        onViewFilm(it)
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

                        if (items().isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyDataMessage(
                                    modifier = Modifier
                                        .padding(top = 25.dp)
                                        .background(MaterialTheme.colorScheme.surface),
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.padding(LocalGlobalScaffoldPadding.current))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteItemAlert || showDeleteSelectionAlert) {
        val alertDescription =
            if (showDeleteItemAlert) {
                val itemName = uiState.longClickedItem?.metadata?.title ?: ""
                stringResource(LocaleR.string.warn_delete_library_format, itemName)
            } else {
                stringResource(LocaleR.string.warn_delete_selected_libraries_format)
            }

        IconAlertDialog(
            painter = painterResource(UiCommonR.drawable.warning_outline),
            contentDescription = null,
            description = alertDescription,
            onConfirm = {
                if (showDeleteItemAlert) {
                    onRemoveLongClickedItem()
                } else {
                    onRemoveSelection()
                }
            },
            onDismiss = {
                showDeleteItemAlert = false
                showDeleteSelectionAlert = false
            },
        )
    }
}

@Preview
@Composable
private fun LibraryDetailsScreenBasePreview() {
    val sampleList =
        remember {
            LibraryList(
                id = 1,
                ownerId = 1,
                name = "Best horror movies",
                description = "A curation of the best horror movies out there. Feel free to browse my list :D",
                createdAt = Date(),
                updatedAt = Date(),
            )
        }

    var uiState by remember { mutableStateOf(LibraryDetailsUiState(isLoading = true)) }
    var searchQuery by remember { mutableStateOf("") }
    val films = remember { mutableStateListOf<LibraryListItemWithMetadata>() }
    var selectedItems by remember { mutableStateOf(persistentSetOf<LibraryListItemWithMetadata>()) }

    val safeItems by remember {
        derivedStateOf {
            val list =
                if (searchQuery.isNotEmpty()) {
                    films.filter {
                        it.metadata.title.contains(searchQuery, true)
                            || it.metadata.overview?.contains(searchQuery, true) == true
                    }
                } else {
                    films
                }

            val sortedList =
                list.sortedWith(
                    compareBy<LibraryListItemWithMetadata>(
                        selector = {
                            when (uiState.selectedFilter) {
                                LibrarySortFilter.Name -> it.metadata.title
                                LibrarySortFilter.AddedAt -> it.item.addedAt.time
                                LibraryDetailsFilters.Rating -> it.metadata.rating
                                LibraryDetailsFilters.Year -> it.metadata.year
                                else -> throw Error()
                            }
                        },
                    ).let { comparator ->
                        if (uiState.isSortingAscending) comparator else comparator.reversed()
                    },
                )

            sortedList.toPersistentList()
        }
    }

    LaunchedEffect(true) {
        films.addAll(
            List(100) {
                val film = DummyDataForPreview.getMovie(
                    id = "${it + 1}",
                    title = "Film $it",
                )

                LibraryListItemWithMetadata(
                    metadata = film.toDBFilm(),
                    item = LibraryListItem(
                        id = it.toLong(),
                        filmId = film.identifier,
                        listId = sampleList.id,
                        addedAt = Date(System.currentTimeMillis() - it * 10000000L),
                    )
                )
            },
        )

        delay(3000)
        uiState = uiState.copy(isLoading = false)
    }

    ProvideAsyncImagePreviewHandler {
        FlixclusiveTheme {
            Surface {
                LibraryDetailsScreen(
                    library = sampleList,
                    onGoBack = {},
                    uiState = uiState,
                    items = { safeItems },
                    selectedItems = { selectedItems },
                    searchQuery = { searchQuery },
                    onRemoveSelection = {
                        selectedItems.forEach { film ->
                            films.removeIf { it.metadata.identifier == film.metadata.identifier }
                        }
                    },
                    onStartMultiSelecting = { uiState = uiState.copy(isMultiSelecting = true) },
                    onUnselectAll = {
                        uiState = uiState.copy(isMultiSelecting = false)
                        selectedItems = persistentSetOf()
                    },
                    onAddItems = {},
                    onRemoveLongClickedItem = {
                        val filmToRemove = uiState.longClickedItem
                        films.removeIf { filmToRemove?.metadata?.identifier == it.metadata.identifier }

                        uiState = uiState.copy(longClickedItem = null)
                    },
                    onViewFilm = {},
                    onQueryChange = { searchQuery = it },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                    onToggleSelect = {
                        if (selectedItems.contains(it)) {
                            selectedItems.remove(it)
                        } else {
                            selectedItems.add(it)
                        }
                    },
                    onLongClickItem = { uiState = uiState.copy(longClickedItem = it) },
                    onUpdateFilter = {
                        uiState = if (uiState.selectedFilter == it) {
                            uiState.copy(isSortingAscending = !uiState.isSortingAscending)
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
