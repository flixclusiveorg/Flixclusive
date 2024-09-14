package com.flixclusive.feature.mobile.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.navigation.navigator.FilmScreenNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.core.ui.mobile.component.RetryButton
import com.flixclusive.core.ui.mobile.component.film.FilmCard
import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.feature.mobile.film.component.EpisodeCard
import com.flixclusive.feature.mobile.film.component.EpisodeCardPlaceholder
import com.flixclusive.feature.mobile.film.component.FilmOverview
import com.flixclusive.feature.mobile.film.component.FilmScreenButtons
import com.flixclusive.feature.mobile.film.component.FilmScreenHeader
import com.flixclusive.feature.mobile.film.component.TvShowSeasonDropdown
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

internal enum class FilmTab(val stringId: Int) {
    Episodes(LocaleR.string.episodes),
    MoreLikeThis(LocaleR.string.more_like_this),
    Collections(LocaleR.string.collections)
}

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class
)
@Composable
internal fun FilmScreen(
    navigator: FilmScreenNavigator,
    previewFilm: (Film) -> Unit,
    play: (Film, Episode?) -> Unit,
) {
    val viewModel: FilmScreenViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val film by viewModel.film.collectAsStateWithLifecycle()
    val currentSeasonSelected by viewModel.currentSeasonSelected.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()

    val listState = rememberLazyGridState()
    val isCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

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

            RetryButton(
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
                    val filmTabs = remember {
                        val filmTabs = mutableListOf<FilmTab>()

                        if (film.filmType == FilmType.TV_SHOW) {
                            filmTabs.add(FilmTab.Episodes)
                        }

                        if (film.recommendations.isNotEmpty()) {
                            filmTabs.add(FilmTab.MoreLikeThis)
                        }

                        if (film is Movie && film.collection?.films?.isNotEmpty() == true) {
                            filmTabs.add(FilmTab.Collections)
                        }

                        filmTabs
                    }
                    val (currentTabSelected, onTabChange) = rememberSaveable { mutableStateOf(filmTabs.firstOrNull()) }

                    val catalogueToUse = rememberSaveable(currentTabSelected) {
                        when (currentTabSelected) {
                            FilmTab.MoreLikeThis -> film.recommendations
                            FilmTab.Collections -> (film as Movie).collection!!.films
                            else -> emptyList()
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        state = listState
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmScreenHeader(
                                film = film,
                                onGenreClick = {
                                    if (it.id == -1)
                                        return@FilmScreenHeader

                                    navigator.openGenreScreen(it)
                                },
                                onNavigateClick = navigator::goBack
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmScreenButtons(
                                modifier = Modifier
                                    .padding(horizontal = 15.dp)
                                    .padding(top = 20.dp),
                                isInWatchlist = state.isFilmInWatchlist,
                                releaseStatus = film.releaseStatus,
                                watchHistoryItem = watchHistoryItem,
                                onPlayClick = {
                                    play(film, null)
                                },
                                onWatchlistClick = viewModel::onWatchlistButtonClick
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilmOverview(
                                overview = film.overview,
                                defaultExpandState = filmTabs.isEmpty(),
                                modifier = Modifier
                                    .padding(horizontal = 15.dp)
                                    .padding(top = 25.dp)
                            )
                        }

                        if (filmTabs.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ContentTabs(
                                    modifier = Modifier
                                        .padding(top = 20.dp, bottom = 10.dp),
                                    filmTabs = filmTabs,
                                    currentTabSelected = filmTabs.indexOf(currentTabSelected),
                                    onTabChange = {
                                        onTabChange(filmTabs[it])
                                    }
                                )
                            }
                        }

                        val isTvShowAndIsTabSelected = film.filmType == FilmType.TV_SHOW
                                && currentTabSelected == FilmTab.Episodes
                        if (isTvShowAndIsTabSelected) {
                            val tvShow = film as TvShow
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TvShowSeasonDropdown(
                                    modifier = Modifier
                                        .padding(vertical = 5.dp),
                                    seasons = tvShow.seasons,
                                    selectedSeason = viewModel.selectedSeasonNumber,
                                    onSeasonChange = {
                                        viewModel.onSeasonChange(it)
                                    }
                                )
                            }

                            when (currentSeasonSelected) {
                                is Resource.Failure -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        val seasonErrorMessage =
                                            currentSeasonSelected.error!!.asString()

                                        RetryButton(
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

                                Resource.Loading -> {
                                    items(
                                        count = 3,
                                        span = { GridItemSpan(maxLineSpan) },
                                    ) {
                                        EpisodeCardPlaceholder(modifier = Modifier.padding(vertical = 5.dp))
                                    }
                                }

                                is Resource.Success -> {
                                    items(
                                        items = currentSeasonSelected.data!!.episodes,
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = { it }
                                    ) { episode ->
                                        EpisodeCard(
                                            modifier = Modifier
                                                .padding(vertical = 5.dp),
                                            episode = episode,
                                            watchHistoryItem = watchHistoryItem,
                                            onEpisodeClick = { episodeToWatch ->
                                                play(film, episodeToWatch)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = catalogueToUse,
                                key = { film -> film.identifier }
                            ) { film ->
                                FilmCard(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .animateItemPlacement(),
                                    isShowingTitle = appSettings.isShowingFilmCardTitle,
                                    film = film,
                                    onClick = navigator::openFilmScreen,
                                    onLongClick = previewFilm
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

        CollapsibleTopBar(
            title = film?.title ?: "",
            isCollapsedProvider = { isCollapsed || state.isLoading || state.errorMessage != null },
            onNavigationIconClick = navigator::goBack,
        )
    }
}

@Composable
private fun ContentTabs(
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
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F)
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                        )
                    },
                    selected = isSelected,
                    onClick = { onTabChange(index) }
                )
            }
        }
    }
}

@Composable
private fun CollapsibleTopBar(
    title: String,
    isCollapsedProvider: () -> Boolean,
    onNavigationIconClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    val isCollapsed by rememberUpdatedState(newValue = isCollapsedProvider())

    AnimatedVisibility(
        visible = isCollapsed,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    drawRect(surfaceColor)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = stringResource(LocaleR.string.navigate_up)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp)
                )
            }
        }
    }
}