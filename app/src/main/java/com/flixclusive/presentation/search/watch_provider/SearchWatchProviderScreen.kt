package com.flixclusive.presentation.search.watch_provider

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.domain.model.tmdb.WatchProvider
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.composables.VerticalGridWithFiltersScreen
import com.flixclusive.presentation.common.shouldPaginate
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.SearchFilmScreenDestination
import com.flixclusive.presentation.main.MainSharedViewModel
import com.flixclusive.presentation.search.SearchNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@SearchNavGraph
@Destination(
    style = CommonScreenTransition::class
)
@Composable
fun SearchWatchProviderScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    item: WatchProvider
) {
    val viewModel: SearchWatchProviderViewModel = hiltViewModel()
    val listState = rememberLazyGridState()

    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }

    LaunchedEffect(key1 = shouldStartPaginate) {
        if(shouldStartPaginate && viewModel.pagingState == PagingState.IDLE)
            viewModel.getFilms()
    }

    VerticalGridWithFiltersScreen(
        listState =  listState,
        pagingState = viewModel.pagingState,
        currentFilter = viewModel.currentFilterSelected,
        screenTitle = item.providerName,
        films = viewModel.films,
        onFilterChange = viewModel::onFilterChange,
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onNavigationIconClick = navigator::navigateUp,
        onRetry = {
            viewModel.resetPagingState()
            viewModel.getFilms()
        },
        onFilmClick = { clickedFilm ->
            navigator.navigate(
                SearchFilmScreenDestination(
                    film = clickedFilm
                ),
                onlyIfResumed = true
            )
        },
    )
}