package com.flixclusive.feature.tv.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.home.HomeScreenViewModel
import com.flixclusive.core.ui.tv.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.feature.tv.home.component.HomeFilmsRow
import com.flixclusive.feature.tv.home.component.ImmersiveHomeBackground
import com.flixclusive.model.tmdb.Film
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay

internal const val HOME_FOCUS_KEY_FORMAT = "row=%d, column=%d"

@Destination
@Composable
fun HomeScreen(
    navigator: CommonScreenNavigator
) {
    val viewModel: HomeScreenViewModel = hiltViewModel()

    val currentRoute = useLocalCurrentRoute()

    val listState = rememberTvLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val headerItem by viewModel.headerItem.collectAsStateWithLifecycle()
    val homeCategories by viewModel.homeCategories.collectAsStateWithLifecycle()
    val homeRowItems by viewModel.homeRowItems.collectAsStateWithLifecycle()
    val homeRowItemsPagingState by viewModel.homeRowItemsPagingState.collectAsStateWithLifecycle()
    val lastItemFocused = useLocalLastFocusedItemPerDestination()

    var focusedFilm: Film? by remember { mutableStateOf(null) }
    val backgroundHeight = 400.dp

    val topFade = Brush.verticalGradient(
        0.48F to Color.Transparent,
        0.5F to Color.Red
    )

    // Initialize focus to the first item.
    LaunchedEffect(Unit) {
        val defaultValue = String.format(HOME_FOCUS_KEY_FORMAT, 0, 0)
        lastItemFocused.getOrPut(currentRoute) {
            // Pre-load the focused film.
            homeRowItems.getOrNull(0)
                ?.getOrNull(0)
                ?.let {
                    viewModel.loadFocusedFilm(it)
                }

            defaultValue
        }
    }

    LaunchedEffect(focusedFilm) {
        delay(800)
        focusedFilm?.let {
            viewModel.loadFocusedFilm(it)
        }
    }

    LocalFocusTransferredOnLaunchProvider {
        Box {
            ImmersiveHomeBackground(
                headerItem = headerItem,
                backgroundHeight = backgroundHeight,
                modifier = Modifier
                    .padding(start = useLocalDrawerWidth())
            )

            Box(
                modifier = Modifier
                    .fadingEdge(topFade)
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
                        pivotOffsets = PivotOffsets(0.55F),
                        state = listState,
                    ) {
                        items(3) {
                            NonFocusableSpacer(height = 110.dp)
                        }

                        items(
                            count = viewModel.itemsSize,
                            key = { it % homeCategories.size }
                        ) { i ->
                            val rowIndex = i % homeCategories.size
                            val category = homeCategories[rowIndex]

                            HomeFilmsRow(
                                categoryItem = category,
                                paginationState = homeRowItemsPagingState[rowIndex],
                                films = homeRowItems[rowIndex],
                                rowIndex = i,
                                onFilmClick = navigator::openFilmScreen,
                                onFocusedFilmChange = { film ->
                                    focusedFilm = film
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
}