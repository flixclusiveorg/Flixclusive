package com.flixclusive.presentation.mobile.common.composables.see_all

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.viewmodels.see_all.SeeAllViewModel
import com.flixclusive.presentation.mobile.common.composables.FilmsGridScreen
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate

@Composable
fun SeeAllMobileScreen(
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onNavigationIconClick: (() -> Unit)? = null,
) {
    val viewModel: SeeAllViewModel = hiltViewModel()

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

    FilmsGridScreen(
        listState =  listState,
        pagingState = viewModel.pagingState,
        currentFilter = if(viewModel.filmTypeCouldBeBoth) viewModel.currentFilterSelected else null,
        screenTitle = viewModel.itemConfig.name,
        films = viewModel.films,
        onRetry = {
            viewModel.resetPagingState()
            viewModel.getFilms()
        },
        onFilterChange = viewModel::onFilterChange,
        onFilmLongClick = onFilmLongClick,
        onNavigationIconClick = onNavigationIconClick,
        onFilmClick = onFilmClick,
    )
}