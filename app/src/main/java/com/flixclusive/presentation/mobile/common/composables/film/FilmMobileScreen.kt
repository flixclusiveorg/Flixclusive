package com.flixclusive.presentation.mobile.common.composables.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenViewModel
import com.flixclusive.presentation.mobile.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile

enum class FilmTab(val stringId: Int) {
    Episodes(R.string.episodes),
    MoreLikeThis(R.string.more_like_this)
}

@UnstableApi
@Composable
fun FilmMobileScreen(
    onNavigationIconClick: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onFilmClick: (Film) -> Unit,
    onPlayClick: (Film, TMDBEpisode?) -> Unit,
) {
    val viewModel: FilmScreenViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val film by viewModel.film.collectAsStateWithLifecycle()
    val currentSeasonSelected by viewModel.currentSeasonSelected.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()

    val (currentTabSelected, onTabChange) = rememberSaveable { mutableIntStateOf(0) }

    val listState = rememberLazyGridState()
    val isCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val isTvShowAndIsTabSelected = film?.filmType == FilmType.TV_SHOW && currentTabSelected == 0

    Box {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilmScreenPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            ErrorScreenWithButton(
                shouldShowError = state.errorMessage != null,
                error = state.errorMessage?.asString(),
                modifier = Modifier
                    .fillMaxSize(),
                onRetry = viewModel::initializeData
            )

            AnimatedVisibility(
                visible = !state.isLoading && state.errorMessage == null && film != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                film?.let { film ->
                    val filmTabs = mutableListOf<FilmTab>()

                    if (film.filmType == FilmType.TV_SHOW) {
                        filmTabs.add(FilmTab.Episodes)
                    }

                    if (film.recommendedTitles.isNotEmpty()) {
                        filmTabs.add(FilmTab.MoreLikeThis)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        state = listState
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmHeader(
                                film = film,
                                onGenreClick = onGenreClick,
                                onNavigateClick = onNavigationIconClick
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmButtons(
                                modifier = Modifier
                                    .padding(horizontal = LABEL_START_PADDING)
                                    .padding(top = 10.dp),
                                isInWatchlistProvider = { state.isFilmInWatchlist },
                                watchHistoryItem = watchHistoryItem,
                                onPlayClick = {
                                    onPlayClick(film, null)
                                },
                                onWatchlistClick = viewModel::onWatchlistButtonClick
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmOverview(
                                overview = film.overview,
                                modifier = Modifier
                                    .padding(horizontal = LABEL_START_PADDING)
                                    .padding(top = 25.dp)
                            )
                        }

                        if (filmTabs.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FilmTabsChildScreen(
                                    modifier = Modifier
                                        .padding(top = 20.dp, bottom = 10.dp),
                                    filmTabs = filmTabs,
                                    currentTabSelected = currentTabSelected,
                                    onTabChange = onTabChange
                                )
                            }
                        }

                        if (isTvShowAndIsTabSelected) {
                            val tvShow = film as TvShow
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FilmSeasons(
                                    modifier = Modifier
                                        .padding(vertical = 5.dp),
                                    seasons = tvShow.seasons,
                                    selectedSeasonProvider = { viewModel.selectedSeasonNumber },
                                    onSeasonChange = {
                                        viewModel.onSeasonChange(it)
                                    }
                                )
                            }

                            if (currentSeasonSelected is Resource.Loading) {
                                items(
                                    count = 3,
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    LoadingFilmEpisode(modifier = Modifier.padding(vertical = 5.dp))
                                }
                            }

                            if (currentSeasonSelected is Resource.Failure) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    val seasonErrorMessage = currentSeasonSelected.error?.asString()
                                        ?: UiText.StringValue("Failed to fetch season ${viewModel.selectedSeasonNumber}")
                                            .asString()

                                    ErrorScreenWithButton(
                                        modifier = Modifier
                                            .height(400.dp)
                                            .fillMaxWidth(),
                                        shouldShowError = true,
                                        error = seasonErrorMessage,
                                        onRetry = {
                                            viewModel.onSeasonChange(viewModel.selectedSeasonNumber)
                                        }
                                    )
                                }
                            }

                            if (currentSeasonSelected is Resource.Success) {
                                items(
                                    items = currentSeasonSelected.data!!.episodes,
                                    key = { it.episode },
                                    span = { GridItemSpan(maxLineSpan) },
                                    contentType = { it }
                                ) { episode ->
                                    FilmEpisode(
                                        modifier = Modifier
                                            .padding(vertical = 5.dp),
                                        episode = episode,
                                        watchHistoryItem = watchHistoryItem,
                                        onEpisodeClick = { episodeToWatch ->
                                            onPlayClick(film, episodeToWatch)
                                        }
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = film.recommendedTitles,
                                key = { i, film ->
                                    film.id * i
                                }
                            ) { _, film ->
                                FilmCard(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    shouldShowTitle = appSettings.isShowingFilmCardTitle,
                                    film = film,
                                    onClick = onFilmClick,
                                    onLongClick = onFilmLongClick
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(25.dp))
                        }
                    }
                }
            }
        }

        CollapsedFilmTopAppBar(
            filmTitle = film?.title ?: "",
            isCollapsedProvider = { isCollapsed || state.isLoading || state.errorMessage != null },
            onNavigationIconClick = onNavigationIconClick,
        )
    }
}

@Composable
fun FilmTabsChildScreen(
    modifier: Modifier = Modifier,
    filmTabs: List<FilmTab>,
    currentTabSelected: Int,
    onTabChange: (Int) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        TabRow(
            selectedTabIndex = currentTabSelected,
            divider = {
                Divider(
                    color = colorOnMediumEmphasisMobile(emphasis = 0.2F),
                    thickness = 0.5.dp
                )
            },
            indicator = {
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(it[currentTabSelected])
                        .padding(horizontal = 65.dp)
                )
            }
        ) {
            filmTabs.forEachIndexed { index, filmTab ->
                val isSelected = currentTabSelected == index

                Tab(
                    text = {
                        Text(
                            text = stringResource(id = filmTab.stringId),
                            color = if(isSelected) MaterialTheme.colorScheme.primary else Color.White
                        )
                    },
                    selected = isSelected,
                    onClick = { onTabChange(index) }
                )
            }
        }
    }
}