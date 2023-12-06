package com.flixclusive.presentation.mobile.common.composables.genre

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.mobile.common.composables.FilmsGridScreen
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate


data class GenreScreenNavArgs(
    val genre: Genre,
)
@Composable
fun GenreScreen(
    genre: Genre,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onNavigationIconClick: () -> Unit,
) {
    val viewModel: GenreViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
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
        isShowingFilmCardTitle = appSettings.isShowingFilmCardTitle,
        currentFilter = if(viewModel.filmTypeCouldBeBoth) viewModel.currentFilterSelected else null,
        screenTitle = genre.name,
        films = viewModel.films,
        onFilterChange = viewModel::onFilterChange,
        onFilmLongClick = onFilmLongClick,
        onNavigationIconClick = onNavigationIconClick,
        onRetry = {
            viewModel.resetPagingState()
            viewModel.getFilms()
        },
        onFilmClick = onFilmClick,
    )
}