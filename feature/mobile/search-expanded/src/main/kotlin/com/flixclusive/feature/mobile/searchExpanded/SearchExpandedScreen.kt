package com.flixclusive.feature.mobile.searchExpanded

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.pagination.PagingState
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewFilmAction
import com.flixclusive.core.presentation.mobile.util.shouldPaginate
import com.flixclusive.feature.mobile.searchExpanded.component.SearchBarInput
import com.flixclusive.feature.mobile.searchExpanded.component.SearchFilmsGridView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchProvidersView
import com.flixclusive.feature.mobile.searchExpanded.component.SearchSearchHistoryView
import com.flixclusive.feature.mobile.searchExpanded.component.filter.FilterBottomSheet
import com.flixclusive.feature.mobile.searchExpanded.util.Constant
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.isBeingUsed
import com.flixclusive.model.film.Film
import com.flixclusive.provider.filter.FilterList
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

interface SearchExpandedScreenNavigator : GoBackAction, ViewFilmAction
internal enum class SearchItemViewType {
    SearchHistory,
    Providers,
    Films,
}

@Destination
@Composable
internal fun SearchExpandedScreen(
    navigator: SearchExpandedScreenNavigator,
    viewModel: SearchExpandedScreenViewModel = hiltViewModel(),
    previewFilm: (Film) -> Unit,
) {
    val providerMetadataList = viewModel.providerMetadataList

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }

    val uiPreferences by viewModel.uiPreferences.collectAsStateWithLifecycle()

    var filterGroupIndexToShow by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(key1 = shouldStartPaginate) {
        if (shouldStartPaginate && viewModel.pagingState == com.flixclusive.core.common.pagination.PagingState.IDLE) {
            viewModel.paginateItems()
        }
    }

    val providerMetadata =
        remember(viewModel.selectedProviderId) {
            val providerMetadata =
                providerMetadataList.find { viewModel.selectedProviderId == it.id }

            if (providerMetadata == null) {
                return@remember Constant.tmdbProviderMetadata
            }

            providerMetadata
        }

    val sortedFilters by remember {
        derivedStateOf {
            FilterList(viewModel.filters.sortedByDescending { it.isBeingUsed() })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBarInput(
                searchQuery = viewModel.searchQuery,
                lastQuerySearched = viewModel.lastQuerySearched,
                currentViewType = viewModel.currentViewType,
                providerMetadata = providerMetadata,
                filters = sortedFilters,
                onNavigationIconClick = navigator::goBack,
                onQueryChange = viewModel::onQueryChange,
                onToggleFilterSheet = { filterGroupIndexToShow = it },
                onSearch = {
                    scope.launch {
                        // Scroll to top
                        listState.scrollToItem(0)
                    }
                    viewModel.onSearch()
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = viewModel.currentViewType.value,
            transitionSpec = {
                val enter =
                    when (targetState) {
                        SearchItemViewType.Films -> slideInHorizontally { it } + fadeIn()
                        SearchItemViewType.Providers -> slideInHorizontally { -it } + fadeIn()
                        else -> fadeIn()
                    }

                val exit =
                    when (initialState) {
                        SearchItemViewType.Films -> slideOutHorizontally { it } + fadeOut()
                        SearchItemViewType.Providers -> slideOutHorizontally { -it } + fadeOut()
                        else -> fadeOut()
                    }

                enter togetherWith exit
            },
            label = "",
        ) { viewType ->
            val modifier =
                Modifier
                    .padding(innerPadding)
                    .clip(RoundedCornerShape(topEnd = 4.dp, topStart = 4.dp))

            when (viewType) {
                SearchItemViewType.SearchHistory -> {
                    SearchSearchHistoryView(
                        modifier = modifier,
                        searchHistory = viewModel.searchHistory.collectAsStateWithLifecycle().value,
                        onSearch = viewModel::onSearch,
                        onQueryChange = viewModel::onQueryChange,
                        deleteSearchHistoryItem = viewModel::deleteSearchHistoryItem,
                    )
                }
                SearchItemViewType.Providers -> {
                    SearchProvidersView(
                        modifier = modifier,
                        providerMetadataList = viewModel.providerMetadataList,
                        selectedProviderId = viewModel.selectedProviderId,
                        onChangeProvider = viewModel::onChangeProvider,
                    )
                }
                SearchItemViewType.Films -> {
                    SearchFilmsGridView(
                        modifier = modifier,
                        uiPreferences = uiPreferences,
                        listState = listState,
                        previewFilm = previewFilm,
                        openFilmScreen = navigator::openFilmScreen,
                        searchResults = viewModel.searchResults,
                        pagingState = viewModel.pagingState,
                        error = viewModel.error,
                        paginateItems = viewModel::paginateItems,
                    )
                }
            }
        }
    }

    if (filterGroupIndexToShow != null) {
        FilterBottomSheet(
            filters = sortedFilters[filterGroupIndexToShow!!],
            onUpdateFilters = { viewModel.onUpdateFilters(sortedFilters) },
            onDismissRequest = {
                filterGroupIndexToShow = null
            },
        )
    }
}
