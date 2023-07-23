package com.flixclusive.presentation.home.content

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.Functions.getNextEpisodeToWatch
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.common.composables.placeholderEffect
import com.flixclusive.presentation.common.transitions.ChildScreenTransition
import com.flixclusive.presentation.destinations.HomeFilmScreenDestination
import com.flixclusive.presentation.destinations.HomeGenreScreenDestination
import com.flixclusive.presentation.destinations.SeeAllScreenDestination
import com.flixclusive.presentation.film.FilmCardPlaceholder
import com.flixclusive.presentation.home.HomeNavGraph
import com.flixclusive.presentation.main.BOTTOM_NAVIGATION_BAR_PADDING
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.presentation.main.MainSharedViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private fun filterWatchedFilms(watchHistoryItem: WatchHistoryItem): Boolean {
    val isTvShow = watchHistoryItem.seasons != null

    var isFinished = true
    if (watchHistoryItem.episodesWatched.isEmpty()) {
        isFinished = false
    } else if(isTvShow) {
        val nextEpisodeToWatch = getNextEpisodeToWatch(watchHistoryItem)
        if(nextEpisodeToWatch.first != null)
            isFinished = false
    } else {
        isFinished = watchHistoryItem.episodesWatched.last().isFinished
    }

    return isFinished
}

@HomeNavGraph(start = true)
@Destination(
    style = ChildScreenTransition::class
)
@Composable
fun HomeScreenContent(
    viewModel: HomeContentViewModel = hiltViewModel(),
    mainSharedViewModel: MainSharedViewModel,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchHistoryItems by viewModel.continueWatchingList.collectAsStateWithLifecycle(emptyList())
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
    
    val seeAllContent = { flag: String, label: String ->
        navigator.navigate(
            direction = SeeAllScreenDestination(
                flag = flag,
                label = label
            ),
            onlyIfResumed = true
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HomeScreenContentLoadingScreen(isLoading = uiState.isLoading && !uiState.hasErrors)

        ErrorScreenWithButton(
            modifier = Modifier
                .fillMaxSize(),
            shouldShowError = uiState.hasErrors,
            error = UiText.StringResource(R.string.error_on_initialization).asString(),
            onRetry = viewModel::initialize
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if(uiState.headerItem != null && !uiState.hasErrors) {
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

                items(viewModel.homeRowItems, key = { it.label.asString(context) }) { item ->
                    HomeItemsRow(
                        flag = item.flag,
                        label = item.label,
                        dataListProvider = { item.data },
                        onFilmClick = navigateToFilm,
                        onFilmLongClick = mainSharedViewModel::onFilmLongClick,
                        onSeeAllClick = seeAllContent
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier
                        .padding(bottom = BOTTOM_NAVIGATION_BAR_PADDING)
                )
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
            HomeHeaderPlaceholder(
                modifier = Modifier
                    .height(480.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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