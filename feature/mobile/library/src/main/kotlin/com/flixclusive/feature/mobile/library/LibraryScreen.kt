package com.flixclusive.feature.mobile.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewFilmAction
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.mobile.component.LoadingScreen
import com.flixclusive.core.ui.mobile.component.PlainTooltipBox
import com.flixclusive.core.ui.mobile.component.topbar.ActionButton
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarWithSearch
import com.flixclusive.core.ui.mobile.component.topbar.DefaultNavigationIcon
import com.flixclusive.core.ui.mobile.component.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.feature.mobile.library.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.feature.mobile.library.component.DefaultLibraryCardShape
import com.flixclusive.feature.mobile.library.component.LibraryCard
import com.flixclusive.feature.mobile.library.component.LibraryFilterBottomSheet
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.film.Film
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface LibraryScreenNavigator :
    GoBackAction,
    ViewFilmAction

@Destination
@Composable
internal fun LibraryScreen(
    navigator: LibraryScreenNavigator,
    viewModel: LibraryScreenViewModel = hiltViewModel(),
    previewFilm: (Film) -> Unit,
) {
}

@Composable
private fun LibraryScreen(
    isLoading: Boolean,
    isSearching: Boolean,
    isShowingFilterSheet: Boolean,
    currentFilter: () -> LibrarySortFilter,
    currentDirection: () -> LibrarySortFilter.Direction,
    libraries: () -> List<LibraryListWithPreview>,
    selectedLibraries: () -> List<LibraryListWithPreview>,
    searchQuery: () -> String,
    onRemoveSelection: () -> Unit,
    onUnselectAll: () -> Unit,
    onViewLibraryContent: (LibraryList) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    onToggleFilterSheet: (Boolean) -> Unit,
    onToggleSelect: (LibraryListWithPreview) -> Unit,
    onModifyLibrary: (LibraryList) -> Unit,
    onUpdateFilter: (LibrarySortFilter) -> Unit,
) {
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()
    val listState = rememberLazyListState()

    val selectedColor = MaterialTheme.colorScheme.tertiary
    val selectCount by remember {
        derivedStateOf { selectedLibraries().size }
    }

    Scaffold(
        topBar = {
            LibraryTopBar(
                isSearching = isSearching,
                isLoading = isLoading,
                selectCount = { selectCount },
                scrollBehavior = scrollBehavior,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
                onShowFilterSheet = { onToggleFilterSheet(true) },
                onRemoveSelection = onRemoveSelection,
                onUnselectAll = onUnselectAll,
            )
        },
    ) {
        val padding by remember {
            derivedStateOf { it }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Snapshot.withoutReadObservation {
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset
                    )
                }

                items(
                    items = libraries(),
                    key = { it.list.id },
                ) { library ->
                    LibraryCard(
                        library = library,
                        onClick = {
                            val isSelecting = selectedLibraries().isNotEmpty()
                            if (isSelecting) {
                                onToggleSelect(library)
                            } else {
                                onViewLibraryContent(library.list)
                            }
                        },
                        onLongClick = { onModifyLibrary(library.list) },
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
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingScreen(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    if (isShowingFilterSheet) {
        LibraryFilterBottomSheet(
            currentFilter = currentFilter(),
            currentDirection = currentDirection(),
            onDismissRequest = { onToggleFilterSheet(false) },
            onUpdateFilter = onUpdateFilter
        )
    }
}

@Composable
private fun LibraryTopBar(
    isSearching: Boolean,
    isLoading: Boolean,
    selectCount: () -> Int,
    searchQuery: () -> String,
    onToggleSearchBar: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onRemoveSelection: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onUnselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title by remember {
        derivedStateOf {
            if (selectCount() > 0) {
                context.getString(LocaleR.string.count_selection_format, selectCount())
            } else {
                context.getString(LocaleR.string.my_library)
            }
        }
    }

    CommonTopBarWithSearch(
        modifier = modifier,
        isSearching = isSearching,
        title = title,
        onNavigate = {},
        navigationIcon = {
            if (selectCount() > 0) {
                PlainTooltipBox(description = stringResource(LocaleR.string.cancel)) {
                    ActionButton(onClick = onUnselectAll) {
                        AdaptiveIcon(
                            painter = painterResource(com.flixclusive.core.ui.common.R.drawable.round_close_24),
                            contentDescription = stringResource(LocaleR.string.cancel),
                        )
                    }
                }
            } else if (isSearching) {
                DefaultNavigationIcon(
                    onClick = { onToggleSearchBar(false) },
                )
            }
        },
        searchQuery = searchQuery,
        onToggleSearchBar = onToggleSearchBar,
        onQueryChange = onQueryChange,
        scrollBehavior = scrollBehavior,
        extraActions = {
            if (selectCount() > 0 && !isLoading) {
                PlainTooltipBox(description = stringResource(LocaleR.string.remove)) {
                    ActionButton(onClick = onRemoveSelection) {
                        AdaptiveIcon(
                            painter = painterResource(UiCommonR.drawable.delete),
                            contentDescription = stringResource(LocaleR.string.remove),
                            dp = 24.dp,
                        )
                    }
                }
            } else {
                PlainTooltipBox(description = stringResource(LocaleR.string.filter_button)) {
                    ActionButton(onClick = onShowFilterSheet) {
                        AdaptiveIcon(
                            painter = painterResource(com.flixclusive.core.ui.common.R.drawable.filter_list),
                            contentDescription = stringResource(LocaleR.string.filter_button),
                            dp = 24.dp,
                        )
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun LibraryScreenBasePreview() {
    var uiState by remember { mutableStateOf(LibraryUiState()) }
    val libraries = remember { mutableStateListOf<LibraryListWithPreview>() }

    val safeLibraries by remember {
        derivedStateOf {
            val query = uiState.searchQuery
            val list = if (query.isNotEmpty()) {
                libraries.filter {
                    it.list.name.contains(query) || it.list.description?.contains(query) == true
                }
            } else libraries

            val sortedList = list.sortedWith(
                compareBy<LibraryListWithPreview>(
                    selector = {
                        when (uiState.selectedFilter) {
                            LibrarySortFilter.Name -> it.list.name
                            LibrarySortFilter.AddedAt -> it.list.createdAt.time
                            LibrarySortFilter.ModifiedAt -> it.list.updatedAt.time
                            LibrarySortFilter.ItemCount -> it.itemsCount
                        }
                    }
                ).let { comparator ->
                    if (uiState.selectedFilterDirection == LibrarySortFilter.Direction.ASC) comparator
                    else comparator.reversed()
                }
            )

            sortedList
        }
    }

    LaunchedEffect(true) {
        libraries.addAll(
            List(10) {
                val previews = List(3) { DBFilm(title = "Film #$it").toPreviewPoster() }
                val description =
                    if (Random.nextBoolean()) "Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum"
                    else null
                val list = LibraryList(
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
        uiState = uiState.copy(isLoading = false)
    }

    FlixclusiveTheme {
        Surface {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ) {
                LibraryScreen(
                    isLoading = uiState.isLoading,
                    isSearching = uiState.isShowingSearchBar,
                    isShowingFilterSheet = uiState.isShowingFilterSheet,
                    currentFilter = { uiState.selectedFilter },
                    currentDirection = { uiState.selectedFilterDirection },
                    libraries = { safeLibraries },
                    selectedLibraries = { uiState.selectedLibraries },
                    searchQuery = { uiState.searchQuery },
                    onUpdateFilter = {
                        if (uiState.selectedFilter == it) {
                            uiState = uiState.copy(selectedFilterDirection = uiState.selectedFilterDirection.toggle())
                        } else {
                            uiState = uiState.copy(selectedFilter = it)
                        }
                    },
                    onModifyLibrary = {},
                    onViewLibraryContent = {},
                    onRemoveSelection = {},
                    onToggleSelect = {},
                    onUnselectAll = { uiState = uiState.copy(selectedLibraries = emptyList()) },
                    onToggleFilterSheet = { uiState = uiState.copy(isShowingFilterSheet = it) },
                    onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                    onToggleSearchBar = { uiState = uiState.copy(isShowingSearchBar = it) },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LibraryScreenCompactLandscapePreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun LibraryScreenMediumPortraitPreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun LibraryScreenMediumLandscapePreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun LibraryScreenExtendedPortraitPreview() {
    LibraryScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun LibraryScreenExtendedLandscapePreview() {
    LibraryScreenBasePreview()
}
