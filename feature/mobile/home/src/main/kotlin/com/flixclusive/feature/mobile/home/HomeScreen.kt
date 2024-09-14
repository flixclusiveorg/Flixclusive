package com.flixclusive.feature.mobile.home

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
import com.flixclusive.core.ui.common.navigation.navigator.HomeNavigator
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.core.ui.home.HomeScreenViewModel
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.film.FilmCardPlaceholder
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun HomeScreen(
    navigator: HomeNavigator,
    previewFilm: (Film) -> Unit,
    play: (Film, Episode?) -> Unit,
) {
    val viewModel: HomeScreenViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val headerItem = uiState.headerItem
    val homeCategories = uiState.catalogs
    val homeRowItemsPagingState = uiState.rowItemsPagingState
    val homeRowItems = uiState.rowItems
    val watchHistoryItems by viewModel.continueWatchingList.collectAsStateWithLifecycle()

    val isScrollToTopEnabled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    BackHandler(
        enabled = isScrollToTopEnabled
    ) {
        scope.launch {
            safeCall { listState.animateScrollToItem(0) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HomeScreenContentLoadingScreen(isLoading = uiState.status.isLoading)

        RetryButton(
            modifier = Modifier.fillMaxSize(),
            shouldShowError = uiState.status.error != null,
            error = uiState.status.error?.asString() ?: stringResource(LocaleR.string.error_on_initialization),
            onRetry = viewModel::initialize
        )

        LazyColumn(
            state = listState
        ) {
            if(uiState.status.data != null) {
                item {
                    HomeHeader(
                        modifier = Modifier
                            .height(480.dp)
                            .fillMaxWidth(),
                        film = headerItem!!,
                        onGenreClick = { genre ->
                            if(genre.id >= 0) {
                                navigator.openGenreScreen(genre = genre)
                            }
                        },
                        onFilmClick = navigator::openFilmScreen,
                        onFilmLongClick = previewFilm
                    )
                }

                item {
                    HomeContinueWatchingRow(
                        dataListProvider = { watchHistoryItems },
                        onFilmClick = { play(it, null) },
                        showCardTitle = appSettings.isShowingFilmCardTitle,
                        onSeeMoreClick = previewFilm,
                    )
                }

                itemsIndexed(
                    items = homeCategories,
                    key = { _, item -> item.name }
                ) { i, item ->
                    HomeFilmsRow(
                        catalogItem = item,
                        paginationState = homeRowItemsPagingState[i],
                        films = homeRowItems[i],
                        onFilmClick = navigator::openFilmScreen,
                        showCardTitle = appSettings.isShowingFilmCardTitle,
                        onFilmLongClick = previewFilm,
                        paginate = {
                            viewModel.onPaginateFilms(
                                catalog = item,
                                index = i,
                                page = it
                            )
                        },
                        onSeeAllClick = {
                            navigator.openSeeAllScreen(item = item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeScreenContentLoadingScreen(
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
                                    .padding(start = 15.dp)
                                    .placeholderEffect()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = 15.dp),
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
                            .padding(horizontal = 15.dp),
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
                                    .padding(3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}