package com.flixclusive.feature.mobile.seeAll

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.core.ui.mobile.component.film.FilmsGridScreen
import com.flixclusive.core.ui.mobile.util.shouldPaginate
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.model.tmdb.Film
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    navArgsDelegate = SeeAllScreenNavArgs::class
)
@Composable
fun SeeAllScreen(
    navigator: CommonScreenNavigator,
    previewFilm: (Film) -> Unit,
) {
    val viewModel: SeeAllViewModel = hiltViewModel()
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
        currentFilter = when {
            viewModel.isProviderCatalog -> null
            viewModel.isMediaTypeDefault -> viewModel.currentFilterSelected
            else -> null
        },
        isShowingFilmCardTitle = appSettings.isShowingFilmCardTitle,
        screenTitle = viewModel.category.name,
        films = viewModel.films,
        onRetry = {
            viewModel.resetPagingState()
            viewModel.getFilms()
        },
        onFilterChange = viewModel::onFilterChange,
        onFilmLongClick = previewFilm,
        onNavigationIconClick = navigator::goBack,
        onFilmClick = navigator::openFilmScreen,
    )
}