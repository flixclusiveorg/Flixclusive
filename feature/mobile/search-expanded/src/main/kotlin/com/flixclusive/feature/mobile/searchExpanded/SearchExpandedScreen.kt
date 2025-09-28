package com.flixclusive.feature.mobile.searchExpanded

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.tmdb.util.TMDBFilters
import com.flixclusive.feature.mobile.searchExpanded.component.SearchBarInput
import com.flixclusive.feature.mobile.searchExpanded.component.SearchFilmsGridView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchProvidersView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchSearchHistoryView
import com.flixclusive.feature.mobile.searchExpanded.component.filter.FilterBottomSheet
import com.flixclusive.feature.mobile.searchExpanded.util.Constant
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.provider.filter.FilterList
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Destination
@Composable
internal fun SearchExpandedScreen(
    navigator: SearchExpandedScreenNavigator,
    viewModel: SearchExpandedScreenViewModel = hiltViewModel(),
    previewFilm: (Film) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showFilmTitles by viewModel.showFilmTitles.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val providerMetadataList = viewModel.providerMetadataList

    SearchExpandedScreenContent(
        uiState = uiState,
        searchQuery = { searchQuery },
        showFilmTitles = showFilmTitles,
        searchHistory = searchHistory,
        searchResults = viewModel.searchResults,
        providerMetadataList = providerMetadataList,
        filters = viewModel.filters,
        onGoBack = navigator::goBack,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onChangeView = viewModel::onChangeView,
        onChangeProvider = viewModel::onChangeProvider,
        onUpdateFilters = viewModel::onUpdateFilters,
        deleteSearchHistoryItem = viewModel::deleteSearchHistoryItem,
        paginateItems = viewModel::paginateItems,
        openFilmScreen = navigator::openFilmScreen,
        previewFilm = previewFilm,
    )
}

@Composable
private fun SearchExpandedScreenContent(
    uiState: SearchUiState,
    searchQuery: () -> String,
    showFilmTitles: Boolean,
    searchHistory: List<SearchHistory>,
    searchResults: ImmutableSet<FilmSearchItem>,
    providerMetadataList: ImmutableList<ProviderMetadata>,
    filters: FilterList,
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

    val providerMetadata = remember(uiState.selectedProviderId) {
        val providerMetadata = providerMetadataList.find { uiState.selectedProviderId == it.id }

        if (providerMetadata == null) {
            return@remember Constant.tmdbProviderMetadata
        }

        providerMetadata
    }

    val sortedFilters by remember {
        derivedStateOf {
            FilterList(filters.sortedByDescending { it.isBeingUsed() })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBarInput(
                searchQuery = searchQuery,
                lastQuerySearched = uiState.lastQuerySearched,
                currentViewType = uiState.currentViewType,
                providerMetadata = providerMetadata,
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
                        providerMetadataList = providerMetadataList,
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
                        pagingState = uiState.pagingState,
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
private fun SearchExpandedScreenBasePreview() {
    val providers = remember {
        List(10) {
            Constant.tmdbProviderMetadata.copy(
                id = "provider_$it",
                name = "Provider $it",
            )
        }.toImmutableList()
    }

    val searchHistory = remember {
        List(10) {
            SearchHistory(
                id = it,
                query = "Search query $it",
                ownerId = 0,
            )
        }
    }

    val films = remember {
        List(20) {
            DummyDataForPreview.getFilm(
                id = "$it",
                title = "Film $it",
            )
        }.toImmutableSet()
    }

    val filters = remember { TMDBFilters.getDefaultTMDBFilters() }

    FlixclusiveTheme {
        Surface {
            SearchExpandedScreenContent(
                uiState = SearchUiState(
                    lastQuerySearched = "Film 1",
                    currentViewType = SearchItemViewType.Films,
                    selectedProviderId = providers.first().id,
                    canPaginate = true,
                ),
                searchQuery = { "Film 1" },
                showFilmTitles = true,
                searchHistory = searchHistory,
                searchResults = films,
                providerMetadataList = providers,
                filters = filters,
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
private fun SearchExpandedScreenCompactLandscapePreview() {
    SearchExpandedScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SearchExpandedScreenMediumPortraitPreview() {
    SearchExpandedScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SearchExpandedScreenMediumLandscapePreview() {
    SearchExpandedScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SearchExpandedScreenExtendedPortraitPreview() {
    SearchExpandedScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SearchExpandedScreenExtendedLandscapePreview() {
    SearchExpandedScreenBasePreview()
}
