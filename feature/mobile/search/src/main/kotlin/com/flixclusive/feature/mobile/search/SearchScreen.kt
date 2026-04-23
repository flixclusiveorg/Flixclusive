package com.flixclusive.feature.mobile.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.search.component.SearchBarInput
import com.flixclusive.feature.mobile.search.component.SearchFilmsGridView
import com.flixclusive.feature.mobile.search.component.SearchProvidersView
import com.flixclusive.feature.mobile.search.component.SearchSearchHistoryView
import com.flixclusive.feature.mobile.search.component.filter.FilterBottomSheet
import com.flixclusive.feature.mobile.search.util.FilterHelper.isBeingUsed
import com.flixclusive.model.film.Film
import com.flixclusive.provider.filter.FilterList
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Destination<ExternalModuleGraph>
@Composable
internal fun SearchScreen(
    navigator: SearchScreenNavigator,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showFilmTitles by viewModel.showFilmTitles.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()

    SearchScreenContent(
        uiState = uiState,
        searchQuery = { searchQuery },
        showFilmTitles = showFilmTitles,
        searchHistory = { searchHistory },
        searchResults = { viewModel.searchResults },
        providers = providers,
        filters = { viewModel.filters },
        onGoBack = navigator::goBack,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onChangeView = viewModel::onChangeView,
        onChangeProvider = viewModel::onChangeProvider,
        onUpdateFilters = viewModel::onUpdateFilters,
        deleteSearchHistoryItem = viewModel::deleteSearchHistoryItem,
        paginateItems = viewModel::paginateItems,
        openFilmScreen = navigator::openFilmScreen,
        previewFilm = navigator::previewFilm,
    )
}

@Composable
private fun SearchScreenContent(
    uiState: SearchUiState,
    showFilmTitles: Boolean,
    searchQuery: () -> String,
    searchHistory: () -> List<SearchHistory>,
    searchResults: () -> Set<Film>,
    providers: Async<List<SearchProvider>>,
    filters: () -> FilterList,
    onGoBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChangeView: (SearchItemViewType) -> Unit,
    onChangeProvider: (String) -> Unit,
    onUpdateFilters: (FilterList) -> Unit,
    deleteSearchHistoryItem: (SearchHistory) -> Unit,
    paginateItems: () -> Unit,
    openFilmScreen: (Film) -> Unit,
    previewFilm: (Film) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    var filterGroupIndexToShow by remember { mutableStateOf<Int?>(null) }

    val updatedPaginateItems by rememberUpdatedState(paginateItems)
    LaunchedEffect(listState, uiState.canPaginate) {
        snapshotFlow { uiState.canPaginate && listState.shouldPaginate() }
            .distinctUntilChanged()
            .collect { shouldPaginate ->
                if (shouldPaginate) {
                    updatedPaginateItems()
                }
            }
    }

    val sortedFilters by remember {
        derivedStateOf {
            FilterList(filters().sortedByDescending { it.isBeingUsed() })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            val provider = remember(providers, uiState.selectedProviderId) {
                if (providers !is Async.Success) return@remember null
                val selectedProvider = uiState.selectedProviderId ?: return@remember null
                providers.data.fastFirstOrNull { selectedProvider == it.id }
            }

            SearchBarInput(
                searchQuery = searchQuery,
                lastQuerySearched = uiState.lastQuerySearched,
                currentViewType = uiState.currentViewType,
                provider = provider,
                filters = sortedFilters,
                onNavigationIconClick = onGoBack,
                onQueryChange = onQueryChange,
                onToggleFilterSheet = { filterGroupIndexToShow = it },
                onChangeView = onChangeView,
                onSearch = {
                    scope.launch {
                        safeCall { listState.scrollToItem(0) }
                    }
                    onSearch()
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentViewType,
            transitionSpec = {
                val enter = when (targetState) {
                    SearchItemViewType.Films -> slideInHorizontally { it } + fadeIn()
                    SearchItemViewType.Providers -> slideInHorizontally { -it } + fadeIn()
                    else -> fadeIn()
                }

                val exit = when (initialState) {
                    SearchItemViewType.Films -> slideOutHorizontally { it } + fadeOut()
                    SearchItemViewType.Providers -> slideOutHorizontally { -it } + fadeOut()
                    else -> fadeOut()
                }

                enter togetherWith exit
            },
        ) { viewType ->
            val modifier = Modifier.clip(RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp))

            when (viewType) {
                SearchItemViewType.History -> {
                    SearchSearchHistoryView(
                        modifier = modifier,
                        searchHistory = searchHistory,
                        scaffoldPadding = innerPadding,
                        onSearch = onSearch,
                        onQueryChange = onQueryChange,
                        deleteSearchHistoryItem = deleteSearchHistoryItem,
                    )
                }

                SearchItemViewType.Providers -> {
                    SearchProvidersView(
                        modifier = modifier,
                        providers = providers,
                        selectedProviderId = uiState.selectedProviderId,
                        onChangeProvider = onChangeProvider,
                        scaffoldPadding = innerPadding,
                    )
                }

                SearchItemViewType.Films -> {
                    SearchFilmsGridView(
                        modifier = modifier,
                        showFilmTitles = showFilmTitles,
                        listState = listState,
                        previewFilm = previewFilm,
                        searchResults = searchResults,
                        pagingState = { uiState.pagingState },
                        error = uiState.error,
                        scaffoldPadding = innerPadding,
                        paginateItems = paginateItems,
                        openFilmScreen = openFilmScreen,
                    )
                }
            }
        }
    }

    if (filterGroupIndexToShow != null) {
        FilterBottomSheet(
            filters = sortedFilters[filterGroupIndexToShow!!],
            onUpdateFilters = { onUpdateFilters(sortedFilters) },
            onDismissRequest = { filterGroupIndexToShow = null },
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
                    isEnabled = it % 2 == 0,
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

    val films = remember {
        List(5) {
            DummyDataForPreview.getFilm(
                id = "$it",
                title = "Film $it",
            )
        }.toSet()
    }

    val filters = remember { FilterList() }

    FlixclusiveTheme {
        Surface {
            SearchScreenContent(
                uiState = SearchUiState(
                    lastQuerySearched = "Film 1",
                    currentViewType = SearchItemViewType.Providers,
                    canPaginate = true,
                ),
                searchQuery = { "Film 1" },
                showFilmTitles = true,
                searchHistory = { searchHistory },
                searchResults = { films },
                providers = providers,
                filters = { filters },
                onGoBack = {},
                onQueryChange = {},
                onSearch = {},
                onChangeView = {},
                onChangeProvider = {},
                onUpdateFilters = {},
                deleteSearchHistoryItem = {},
                paginateItems = {},
                openFilmScreen = {},
                previewFilm = {},
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
