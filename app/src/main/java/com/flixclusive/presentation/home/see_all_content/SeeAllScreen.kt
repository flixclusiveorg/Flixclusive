package com.flixclusive.presentation.home.see_all_content

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.domain.usecase.POPULAR_MOVIE_FLAG
import com.flixclusive.domain.usecase.POPULAR_TV_FLAG
import com.flixclusive.domain.usecase.TOP_MOVIE_FLAG
import com.flixclusive.domain.usecase.TOP_TV_FLAG
import com.flixclusive.domain.usecase.TRENDING_FLAG
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.composables.VerticalGridWithFiltersScreen
import com.flixclusive.presentation.common.shouldPaginate
import com.flixclusive.presentation.common.transitions.CommonScreenTransition
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.home.HomeNavGraph
import com.flixclusive.presentation.main.MainSharedViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@HomeNavGraph
@Destination(
    style = CommonScreenTransition::class
)
@Composable
fun SeeAllScreen(
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    flag: String,
    label: String
) {
    val viewModel: SeeAllViewModel = hiltViewModel()
    val headerTitle = remember {
        when (flag) {
            TRENDING_FLAG -> "Trending"
            TOP_MOVIE_FLAG, TOP_TV_FLAG -> "Top Rated"
            POPULAR_MOVIE_FLAG, POPULAR_TV_FLAG -> "Popular"
            else -> label
        }
    }

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
        screenTitle = headerTitle,
        films = viewModel.films,
        onRetry = {
            viewModel.resetPagingState()
            viewModel.getFilms()
        },
        onFilterChange = viewModel::onFilterChange,
        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
        onNavigationIconClick = navigator::navigateUp,
        onFilmClick = {
            navigator.navigate(
                HomeFilmScreenDestination(
                    film = it
                ),
                onlyIfResumed = true
            )
        },
    )
}