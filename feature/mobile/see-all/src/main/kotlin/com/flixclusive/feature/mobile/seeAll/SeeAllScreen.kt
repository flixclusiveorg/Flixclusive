package com.flixclusive.feature.mobile.seeAll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarWithSearch
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun SeeAllScreen(
    navigator: NavigatorSeeAllScreen,
    navArgs: SeeAllScreenNavArgs,
    viewModel: SeeAllViewModel = hiltViewModel<SeeAllViewModel, SeeAllViewModel.Factory>(
        creationCallback = { it.create(navArgs.catalog) }
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    SeeAllScreenContent(
        items = {
            if (searchQuery.isNotBlank() && uiState.isSearching) {
                viewModel.items.fastFilter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }
            } else {
                viewModel.items
            }
        },
        uiState = { uiState },
        showMediaTitles = showMediaTitles,
        catalog = navArgs.catalog,
        searchQuery = { searchQuery },
        onQueryChange = viewModel::onQueryChange,
        previewMedia = navigator::showMediaPreviewBottomSheet,
        onGoBack = navigator::navigateBack,
        openMediaScreen = navigator::navigateToMediaScreen,
        onToggleSearchBar = viewModel::onToggleSearch,
        paginate = viewModel::paginate,
    )
}

@Composable
private fun SeeAllScreenContent(
    items: () -> List<MediaMetadata>,
    uiState: () -> SeeAllUiState,
    showMediaTitles: Boolean,
    catalog: Catalog,
    searchQuery: () -> String,
    onQueryChange: (String) -> Unit,
    previewMedia: (MediaMetadata) -> Unit,
    onGoBack: () -> Unit,
    onToggleSearchBar: (Boolean) -> Unit,
    openMediaScreen: (MediaMetadata) -> Unit,
    paginate: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    val updatedPaginateItems by rememberUpdatedState(paginate)
    LaunchedEffect(listState, uiState) {
        snapshotFlow { uiState().canPaginate && listState.shouldPaginate() }
            .distinctUntilChanged()
            .collect { shouldPaginate ->
                if (shouldPaginate) {
                    updatedPaginateItems()
                }
            }
    }

    val canScrollUpTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    BackHandler(enabled = canScrollUpTop) {
        scope.launch {
            runCatching { listState.animateScrollToItem(0) }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            CommonTopBarWithSearch(
                title = catalog.name,
                onNavigate = onGoBack,
                scrollBehavior = scrollBehavior,
                isSearching = uiState().isSearching,
                searchQuery = searchQuery,
                onToggleSearchBar = onToggleSearchBar,
                onQueryChange = onQueryChange,
            )
        },
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(LocalGlobalScaffoldPadding.current),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
            contentPadding = padding,
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = items(),
                key = { media -> media.id },
            ) { media ->
                MediaCard(
                    isShowingTitle = showMediaTitles,
                    media = media,
                    onClick = openMediaScreen,
                    onLongClick = previewMedia,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                )
            }

            if (uiState().pagingState.isLoading) {
                items(20) {
                    MediaCardPlaceholder(
                        modifier = Modifier
                            .padding(3.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            (uiState().pagingState as? PagingState.Error)?.let { errorState ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    RetryButton(
                        error = errorState.error.asString(),
                        onRetry = paginate,
                        modifier = Modifier.aspectRatio(MediaCover.Backdrop.ratio),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SeeAllScreenBasePreview() {
    val medias = remember {
        (1..20)
            .map {
                DummyDataForPreview.getMedia(
                    id = it.toString(),
                    title = "MediaMetadata $it",
                )
            }
    }
    var searchQuery by remember { mutableStateOf("") }
    var uiState by remember {
        mutableStateOf(
            SeeAllUiState(
                page = 1,
                maxPage = 1,
                canPaginate = false,
                pagingState = PagingState.Exhausted,
            ),
        )
    }

    FlixclusiveTheme {
        Surface {
            SeeAllScreenContent(
                items = {
                    if (searchQuery.isBlank()) {
                        medias
                    } else {
                        medias
                            .filter {
                                it.title.contains(searchQuery, ignoreCase = true)
                            }
                    }
                },
                uiState = { uiState },
                showMediaTitles = true,
                catalog = remember {
                    Catalog(
                        name = "Netflix",
                        image = null,
                        url = "",
                        canPaginate = true,
                        providerId = "netflix",
                    )
                },
                searchQuery = { searchQuery },
                onQueryChange = { searchQuery = it },
                previewMedia = {},
                onGoBack = {},
                openMediaScreen = {},
                onToggleSearchBar = {
                    uiState = uiState.copy(isSearching = it)
                },
                paginate = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SeeAllScreenCompactLandscapePreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SeeAllScreenMediumPortraitPreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SeeAllScreenMediumLandscapePreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SeeAllScreenExtendedPortraitPreview() {
    SeeAllScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SeeAllScreenExtendedLandscapePreview() {
    SeeAllScreenBasePreview()
}
