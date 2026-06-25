package com.flixclusive.feature.mobile.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.ViewModelUtil.activityHiltViewModel
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.search.component.SearchBarInput
import com.flixclusive.feature.mobile.search.component.SearchMediasGridView
import com.flixclusive.feature.mobile.search.component.SearchProvidersView
import com.flixclusive.feature.mobile.search.component.SearchSearchHistoryView
import com.flixclusive.feature.mobile.search.component.filter.FilterBottomSheet
import com.flixclusive.feature.mobile.search.util.FilterHelper.isBeingUsed
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.provider.filter.FilterList
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Destination<ExternalModuleGraph>
@Composable
internal fun SearchScreen(
    navigator: NavigatorSearchScreen,
    viewModel: SearchViewModel = activityHiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    SearchScreenContent(
        uiState = { uiState },
        searchQuery = { searchQuery },
        showMediaTitles = showMediaTitles,
        searchHistory = { searchHistory },
        searchResults = { viewModel.searchResults },
        providers = { providers },
        filters = { viewModel.filters },
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onChangeView = viewModel::onChangeView,
        onChangeProvider = viewModel::onChangeProvider,
        onUpdateFilters = viewModel::onUpdateFilters,
        onToggleProvider = viewModel::onToggleProvider,
        onConsumeSearchApiErrors = viewModel::onConsumeSearchApiErrors,
        deleteSearchHistoryItem = viewModel::deleteSearchHistoryItem,
        paginateItems = viewModel::paginate,
        openMediaScreen = navigator::navigateToMediaScreen,
        previewMedia = navigator::showMediaPreviewBottomSheet,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SearchScreenContent(
    showMediaTitles: Boolean,
    uiState: () -> SearchUiState,
    searchQuery: () -> String,
    searchHistory: () -> List<SearchHistory>,
    searchResults: () -> Set<MediaMetadata>,
    providers: () -> Async<List<SearchProvider>>,
    filters: () -> FilterList,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChangeView: (SearchViewType) -> Unit,
    onChangeProvider: (String) -> Unit,
    onUpdateFilters: (FilterList) -> Unit,
    onToggleProvider: (SearchProvider) -> Unit,
    onConsumeSearchApiErrors: () -> Unit,
    deleteSearchHistoryItem: (SearchHistory) -> Unit,
    paginateItems: () -> Unit,
    openMediaScreen: (MediaMetadata) -> Unit,
    previewMedia: (MediaMetadata) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val resources = LocalResources.current

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    var filterGroupIndexToShow by remember { mutableStateOf<Int?>(null) }

    val updatedPaginateItems by rememberUpdatedState(paginateItems)
    LaunchedEffect(listState, uiState) {
        snapshotFlow { listState.shouldPaginate() && uiState().pagingState.isIdle }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                updatedPaginateItems()
            }
    }

    val sortedFilters by remember {
        derivedStateOf {
            FilterList(filters().sortedByDescending { it.isBeingUsed() })
        }
    }

    val viewTypes by remember {
        derivedStateOf { uiState().currentViewType }
    }

    val apiErrors by remember {
        derivedStateOf { uiState().searchApiErrors }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(),
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val provider by remember {
                derivedStateOf {
                    val state = providers()
                    if (state !is Async.Success) return@derivedStateOf null
                    val selectedProvider = uiState().selectedProviderId ?: return@derivedStateOf null
                    state.data.fastFirstOrNull { selectedProvider == it.id }
                }
            }

            SearchBarInput(
                searchQuery = searchQuery,
                provider = provider,
                filters = sortedFilters,
                onQueryChange = onQueryChange,
                uiState = uiState,
                onToggleFilterSheet = { filterGroupIndexToShow = it },
                onChangeView = onChangeView,
                onSearch = {
                    if (provider == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = resources.getString(R.string.error_no_selected_provider),
                                withDismissAction = true
                            )
                        }
                        return@SearchBarInput
                    }

                    scope
                        .launch {
                            safeCall { listState.scrollToItem(0) }
                        }.invokeOnCompletion {
                            onSearch()
                        }
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = viewTypes,
            transitionSpec = {
                val enter = when (targetState) {
                    SearchViewType.Medias -> slideInHorizontally { it } + fadeIn()
                    SearchViewType.Providers -> slideInHorizontally { -it } + fadeIn()
                    else -> fadeIn()
                }

                val exit = when (initialState) {
                    SearchViewType.Medias -> slideOutHorizontally { it } + fadeOut()
                    SearchViewType.Providers -> slideOutHorizontally { -it } + fadeOut()
                    else -> fadeOut()
                }

                enter togetherWith exit
            },
        ) { viewType ->
            val modifier = Modifier.clip(RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp))

            when (viewType) {
                SearchViewType.History -> {
                    SearchSearchHistoryView(
                        modifier = modifier,
                        searchHistory = searchHistory,
                        scaffoldPadding = innerPadding,
                        onSearch = onSearch,
                        onQueryChange = onQueryChange,
                        deleteSearchHistoryItem = deleteSearchHistoryItem,
                        onChangeView = onChangeView
                    )
                }

                SearchViewType.Providers -> {
                    SearchProvidersView(
                        modifier = modifier,
                        providers = providers(),
                        selectedProviderId = uiState().selectedProviderId,
                        onChangeProvider = onChangeProvider,
                        onToggleProvider = onToggleProvider,
                        scaffoldPadding = innerPadding,
                    )
                }

                SearchViewType.Medias -> {
                    SearchMediasGridView(
                        modifier = modifier,
                        showMediaTitles = showMediaTitles,
                        listState = listState,
                        previewMedia = previewMedia,
                        searchResults = searchResults,
                        pagingState = { uiState().pagingState },
                        scaffoldPadding = innerPadding,
                        paginateItems = paginateItems,
                        openMediaScreen = openMediaScreen,
                    )
                }
            }
        }
    }

    filterGroupIndexToShow?.let {
        FilterBottomSheet(
            filters = { sortedFilters[it] },
            onUpdateFilters = { onUpdateFilters(sortedFilters) },
            onDismissRequest = { filterGroupIndexToShow = null },
        )
    }

    apiErrors?.let {
        ProviderCrashBottomSheet(
            isLoading = false,
            onDismissRequest = onConsumeSearchApiErrors,
            errors = it,
        )
    }
}

@Preview
@Composable
private fun SearchScreenBasePreview() {
    val providers: Async<List<SearchProvider>> = remember {
//        Async.Loading
        Async.Success(
            List(10) {
                SearchProvider(
                    DummyDataForPreview.getProviderMetadata(
                        id = "$it",
                        name = "Provider $it",
                    ),
                    isSearchEnabled = it % 2 == 0,
                )
            }
        )
    }

    val searchHistory = remember {
        List(10) {
            SearchHistory(
                id = it,
                query = "Search query $it",
                ownerId = "preview-user",
            )
        }
    }

    val medias = remember {
        List(5) {
            DummyDataForPreview.getMedia(
                id = "$it",
                title = "MediaMetadata $it",
            )
        }.toSet()
    }

    val filters = remember { FilterList() }

    FlixclusiveTheme {
        Surface {
            SearchScreenContent(
                uiState = {
                    SearchUiState(
                        lastQuerySearched = "MediaMetadata 1",
                        currentViewType = SearchViewType.Providers,
                        selectedProviderId = (providers as Async.Success).data.first().id,
                    )
                },
                searchQuery = { "MediaMetadata 1" },
                showMediaTitles = true,
                searchHistory = { searchHistory },
                searchResults = { medias },
                providers = { providers },
                filters = { filters },
                onQueryChange = {},
                onSearch = {},
                onChangeView = {},
                onChangeProvider = {},
                onUpdateFilters = {},
                onToggleProvider = {},
                onConsumeSearchApiErrors = {},
                deleteSearchHistoryItem = {},
                paginateItems = {},
                openMediaScreen = {},
                previewMedia = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SearchScreenCompactLandscapePreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SearchScreenMediumPortraitPreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SearchScreenMediumLandscapePreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SearchScreenExtendedPortraitPreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SearchScreenExtendedLandscapePreview() {
    SearchScreenBasePreview()
}
