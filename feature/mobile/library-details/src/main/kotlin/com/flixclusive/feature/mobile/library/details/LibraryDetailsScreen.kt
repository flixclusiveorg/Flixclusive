package com.flixclusive.feature.mobile.library.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.IconAlertDialog
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewFilmAction
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.mobile.component.LoadingScreen
import com.flixclusive.core.ui.mobile.component.film.FilmCard
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.ui.mobile.component.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.feature.mobile.library.common.component.CommonLibraryTopBar
import com.flixclusive.feature.mobile.library.common.component.CommonLibraryTopBarState
import com.flixclusive.feature.mobile.library.common.component.LibraryFilterBottomSheet
import com.flixclusive.feature.mobile.library.common.util.LibraryFilterDirection
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.feature.mobile.library.common.util.selectionBorder
import com.flixclusive.feature.mobile.library.details.component.ScreenHeader
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import java.util.Date
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface LibraryDetailsScreenNavigator :
    ViewFilmAction,
    GoBackAction {
    // TODO: Add navigator to AddLibraryItemScreen
}

data class LibraryDetailsNavArgs(
    val library: LibraryList,
)

@Destination(
    navArgsDelegate = LibraryDetailsNavArgs::class,
)
@Composable
internal fun LibraryDetailsScreen(navigator: LibraryDetailsScreenNavigator) {
}

@Composable
internal fun LibraryDetailsScreen(
    library: LibraryList,
    uiState: () -> LibraryDetailsUiState,
    items: () -> List<FilmWithAddedTime>,
    selectedItems: () -> Set<Film>,
    onGoBack: () -> Unit,
    onRemoveSelection: () -> Unit,
    onStartMultiSelecting: () -> Unit,
    onUnselectAll: () -> Unit,
    onAddItems: () -> Unit,
    onRemoveLongClickedItem: () -> Unit,
    onViewFilm: (Film) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleFilterSheet: (Boolean) -> Unit,
    onToggleSelect: (Film) -> Unit,
    onLongClickItem: (Film) -> Unit,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    val listState = rememberLazyGridState()

    val selectCount by remember {
        derivedStateOf { selectedItems().size }
    }

    val searchQuery by remember {
        derivedStateOf { uiState().searchQuery }
    }

    var showDeleteItemAlert by remember { mutableStateOf(false) }
    var showDeleteSelectionAlert by remember { mutableStateOf(false) }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    var topBarTitleAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState, headerHeightPx) {
        snapshotFlow {
            Triple(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex, headerHeightPx)
        }.collect { (offset, index, headerHeight) ->
            val coercedOffset = offset.coerceIn(0, headerHeight).toFloat()

            topBarTitleAlpha =
                when {
                    index == 0 && headerHeight > coercedOffset -> (coercedOffset / headerHeight).coerceIn(0f, 1f)
                    else -> 1f
                }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val topBarState by remember {
                derivedStateOf {
                    if (uiState().isMultiSelecting) {
                        CommonLibraryTopBarState.Selecting
                    } else if (uiState().isShowingSearchBar) {
                        CommonLibraryTopBarState.Searching
                    } else {
                        CommonLibraryTopBarState.DefaultSubScreen
                    }
                }
            }

            CommonLibraryTopBar(
                topBarState = topBarState,
                scrollBehavior = scrollBehavior,
                isListEmpty = items().isEmpty(),
                onGoBack = onGoBack,
                selectCount = { selectCount },
                searchQuery = { searchQuery },
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onShowFilterSheet = { onToggleFilterSheet(true) },
                onRemoveSelection = { showDeleteSelectionAlert = true },
                onStartMultiSelecting = onStartMultiSelecting,
                onUnselectAll = onUnselectAll,
            ) {
                val title =
                    if (topBarState == CommonLibraryTopBarState.Selecting) {
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
                                    CommonLibraryTopBarState.Selecting -> 1f
                                    else -> topBarTitleAlpha
                                }
                        },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LazyVerticalGrid(
                state = listState,
                columns = GridCells.Adaptive(110.dp),
                contentPadding = paddingValues,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Snapshot.withoutReadObservation {
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    ScreenHeader(
                        library = library,
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .padding(bottom = 25.dp)
                                .onGloballyPositioned {
                                    headerHeightPx = it.size.height
                                },
                    )
                }

                items(
                    items(),
                    key = { it.film.identifier + it.film.providerId },
                ) { (film) ->
                    FilmCard(
                        film = film,
                        onClick = {
                            if (uiState().isMultiSelecting) {
                                onToggleSelect(it)
                            } else {
                                onViewFilm(it)
                            }
                        },
                        onLongClick = onLongClickItem,
                        modifier =
                            Modifier
                                .animateItem()
                                .selectionBorder(
                                    isSelected = selectedItems().contains(film),
                                    shape = MaterialTheme.shapes.extraSmall,
                                ),
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState().isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingScreen(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    }

    if (uiState().isShowingFilterSheet) {
        LibraryFilterBottomSheet(
            filters = LibraryDetailsFilters.defaultFilters,
            currentFilter = uiState().selectedFilter,
            currentDirection = uiState().selectedFilterDirection,
            onDismissRequest = { onToggleFilterSheet(false) },
            onUpdateFilter = onUpdateFilter,
        )
    }

    if (showDeleteItemAlert || showDeleteSelectionAlert) {
        val alertDescription =
            if (showDeleteItemAlert) {
                val itemName = uiState().longClickedFilm?.title ?: ""
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
    val films = remember { mutableStateListOf<FilmWithAddedTime>() }
    val safeItems by remember {
        derivedStateOf {
            val query = uiState.searchQuery
            val list =
                if (query.isNotEmpty()) {
                    films.filter {
                        it.film.title.contains(query, true) || it.film.overview?.contains(query, true) == true
                    }
                } else {
                    films
                }

            val sortedList =
                list.sortedWith(
                    compareBy<FilmWithAddedTime>(
                        selector = {
                            when (uiState.selectedFilter) {
                                LibrarySortFilter.Name -> it.film.title
                                LibrarySortFilter.AddedAt -> it.addedAt.time
                                LibraryDetailsFilters.Rating -> it.film.rating
                                LibraryDetailsFilters.Year -> it.film.year
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
        films.addAll(
            List(100) {
                FilmWithAddedTime.from(
                    film = DBFilm(title = "Film #$it"),
                    addedAt = Date(),
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
                    uiState = { uiState },
                    items = { safeItems },
                    selectedItems = { uiState.selectedItems },
                    onRemoveSelection = {
                        uiState.selectedItems.forEach { film ->
                            films.removeIf { it.film == film }
                        }
                    },
                    onStartMultiSelecting = { uiState = uiState.copy(isMultiSelecting = true) },
                    onUnselectAll = {
                        uiState =
                            uiState.copy(
                                selectedItems = emptySet(),
                                isMultiSelecting = false,
                            )
                    },
                    onAddItems = {},
                    onRemoveLongClickedItem = {
                        val filmToRemove = uiState.longClickedFilm
                        films.removeIf { filmToRemove == it.film }

                        uiState = uiState.copy(longClickedFilm = null)
                    },
                    onViewFilm = {},
                    onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                    onToggleFilterSheet = { uiState = uiState.copy(isShowingFilterSheet = it) },
                    onToggleSelect = {
                        uiState =
                            with(uiState) {
                                val newSet = selectedItems.toMutableSet()

                                if (newSet.contains(it)) {
                                    newSet.remove(it)
                                } else {
                                    newSet.add(it)
                                }

                                copy(selectedItems = newSet.toSet())
                            }
                    },
                    onLongClickItem = { uiState = uiState.copy(longClickedFilm = it) },
                    onUpdateFilter = {
                        uiState = if (uiState.selectedFilter == it) {
                            uiState.copy(selectedFilterDirection = uiState.selectedFilterDirection.toggle())
                        } else uiState.copy(selectedFilter = it)
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
