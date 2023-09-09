package com.flixclusive.presentation.mobile.screens.home.content

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.utils.WatchHistoryUtils.filterWatchedFilms
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.common.viewmodels.home.HomeContentScreenViewModel
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.destinations.HomeGenreScreenDestination
import com.flixclusive.presentation.mobile.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.mobile.common.composables.film.FilmCardPlaceholder
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.main.MainSharedViewModel
import com.flixclusive.presentation.mobile.screens.home.HomeNavGraph
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.flixclusive.presentation.destinations.HomeSeeAllScreenDestination
import kotlinx.coroutines.launch

@HomeNavGraph(start = true)
@Destination(
    style = FadeInAndOutScreenTransition::class
)
@UnstableApi
@Composable
fun HomeScreenContent(
    mainSharedViewModel: MainSharedViewModel,
    navigator: DestinationsNavigator
) {
    val viewModel: HomeContentScreenViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchHistoryItems by viewModel.continueWatchingList.collectAsStateWithLifecycle(emptyList())

    val isScrollToTopEnabled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val isLoading = remember(uiState.isLoading, uiState.hasErrors) { uiState.isLoading && !uiState.hasErrors }
    val recentlyWatchedList = remember(watchHistoryItems) {
        watchHistoryItems
            .filterNot(::filterWatchedFilms)
            .take(10)
    }

    val navigateToFilm = { film: Film ->
        navigator.navigate(
            direction = HomeFilmScreenDestination(
                film = film
            ),
            onlyIfResumed = true
        )
    }

    BackHandler(
        enabled = isScrollToTopEnabled
    ) {
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HomeScreenContentLoadingScreen(isLoading = isLoading)

        ErrorScreenWithButton(
            modifier = Modifier
                .fillMaxSize(),
            shouldShowError = uiState.hasErrors,
            error = stringResource(R.string.error_on_initialization),
            onRetry = viewModel::initialize
        )

        LazyColumn(
            state = listState
        ) {
            if(!isLoading) {
                item {
                    HomeHeader(
                        modifier = Modifier
                            .height(480.dp)
                            .fillMaxWidth(),
                        film = uiState.headerItem!!,
                        onGenreClick = { genre ->
                            if(genre.id >= 0) {
                                navigator.navigate(
                                    HomeGenreScreenDestination(genre = genre),
                                    onlyIfResumed = true
                                )
                            }
                        },
                        onFilmClick = navigateToFilm,
                        onFilmLongClick = mainSharedViewModel::onFilmLongClick
                    )
                }

                item {
                    HomeContinueWatchingRow(
                        dataListProvider = { recentlyWatchedList },
                        onFilmClick = mainSharedViewModel::onPlayClick,
                        onSeeMoreClick = mainSharedViewModel::onFilmLongClick,
                    )
                }

                itemsIndexed(
                    items = viewModel.homeCategories,
                    key = { _, item ->
                        item.name
                    }
                ) { i, item ->
                    HomeMobileFilmsRow(
                        categoryItem = item,
                        paginationState = viewModel.homeRowItemsPagingState[i],
                        films = viewModel.homeRowItems[i],
                        onFilmClick = navigateToFilm,
                        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
                        paginate = { query, page ->
                            viewModel.onPaginate(
                                query = query,
                                page = page,
                                index = i
                            )
                        },
                        onSeeAllClick = {
                            navigator.navigate(
                                onlyIfResumed = true,
                                direction = HomeSeeAllScreenDestination(
                                    item = item
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenContentLoadingScreen(
    isLoading: Boolean
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
            ) {
                HomeHeaderPlaceholder(
                    modifier = Modifier
                        .height(480.dp)
                )
            }

            repeat(3) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1F)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .height(13.dp)
                                    .width(200.dp)
                                    .padding(start = LABEL_START_PADDING)
                                    .placeholderEffect()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = LABEL_START_PADDING),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(24.dp)
                                    .placeholderEffect()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LABEL_START_PADDING),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1F)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .height(20.dp)
                                    .placeholderEffect()
                            )

                            Spacer(
                                modifier = Modifier
                                    .height(14.dp)
                                    .placeholderEffect()
                            )
                        }
                    }

                    Row {
                        repeat(5) {
                            FilmCardPlaceholder(
                                modifier = Modifier
                                    .width(135.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}