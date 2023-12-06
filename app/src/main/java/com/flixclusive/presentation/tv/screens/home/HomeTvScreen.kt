package com.flixclusive.presentation.tv.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.flixclusive.data.usecase.MINIMUM_HOME_ITEMS
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.viewmodels.home.HomeContentScreenViewModel
import com.flixclusive.presentation.destinations.FilmTvScreenDestination
import com.flixclusive.presentation.tv.common.DefaultTvNavArgs
import com.flixclusive.presentation.tv.common.TvRootNavGraph
import com.flixclusive.presentation.tv.main.InitialDrawerWidth
import com.flixclusive.presentation.tv.main.MainTvSharedViewModel
import com.flixclusive.presentation.tv.screens.home.immersive_background.ImmersiveHomeBackground
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

@TvRootNavGraph(start = true)
@Destination(
    navArgsDelegate = DefaultTvNavArgs::class,
)
@UnstableApi
@Composable
fun HomeTvScreen(
    navigator: DestinationsNavigator,
    appViewModel: MainTvSharedViewModel
) {
    val viewModel: HomeContentScreenViewModel = hiltViewModel()

    val listState = rememberTvLazyListState()
    val appState by appViewModel.state.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var focusedFilm: Film? by remember { mutableStateOf(null) }
    var anItemHasBeenFocused by remember { mutableStateOf(false) }
    val backgroundHeight = 400.dp

    LaunchedEffect(Unit) {
        anItemHasBeenFocused = false
    }

    LaunchedEffect(focusedFilm) {
        delay(1000)
        focusedFilm?.let {
            viewModel.loadFocusedFilm(it)
        }
    }

    LaunchedEffect(
        viewModel.itemsSize,
        anItemHasBeenFocused,
        focusedFilm,
        appState.isHidingSplashScreen
    ) {
        val shouldHideSplashScreen = viewModel.itemsSize >= MINIMUM_HOME_ITEMS
                && !appState.isHidingSplashScreen
                && focusedFilm != null && anItemHasBeenFocused

        if(shouldHideSplashScreen) {
            appViewModel.hideSplashScreen()
        }
    }

    Box {
        ImmersiveHomeBackground(
            headerItem = uiState.headerItem,
            backgroundHeight = backgroundHeight,
            modifier = Modifier
                .padding(start = InitialDrawerWidth)
        )

        Box(
            modifier = Modifier
                .padding(top = backgroundHeight.times(0.65F))
        ) {
            if(!uiState.isLoading) {
                val shouldStartPaginate by remember {
                    derivedStateOf {
                        listState.shouldPaginate()
                    }
                }

                LaunchedEffect(shouldStartPaginate) {
                    if(shouldStartPaginate) {
                        viewModel.onPaginateCategories()
                    }
                }

                TvLazyColumn(
                    pivotOffsets = PivotOffsets(0.1F),
                    state = listState,
                ) {
                    items(
                        count = viewModel.itemsSize,
                        key = { it % viewModel.homeCategories.size }
                    ) { rowIndex ->
                        val i = rowIndex % viewModel.homeCategories.size
                        val category = viewModel.homeCategories[i]

                        HomeTvFilmsRow(
                            categoryItem = category,
                            paginationState = viewModel.homeRowItemsPagingState[i],
                            films = viewModel.homeRowItems[i],
                            rowIndex = rowIndex,
                            lastFocusedItem = uiState.lastFocusedItem,
                            anItemHasBeenFocused = anItemHasBeenFocused,
                            onFilmClick = {
                                navigator.navigate(
                                    FilmTvScreenDestination(film = it),
                                    onlyIfResumed = true
                                )
                            },
                            onFocusedFilmChange = { film, filmIndex ->
                                focusedFilm = film
                                anItemHasBeenFocused = true
                                viewModel.onLastItemFocusChange(rowIndex, filmIndex)
                            },
                            paginate = { query, page ->
                                viewModel.onPaginateFilms(
                                    query = query,
                                    page = page,
                                    index = i
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}