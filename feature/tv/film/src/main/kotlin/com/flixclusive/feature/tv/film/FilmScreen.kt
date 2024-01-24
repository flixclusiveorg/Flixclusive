package com.flixclusive.feature.tv.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
import com.flixclusive.core.ui.tv.FadeInAndOutScreenTransition
import com.flixclusive.core.ui.tv.component.FilmOverview
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.drawScrimOnBackground
import com.flixclusive.core.ui.tv.util.useLocalDrawerWidth
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.feature.tv.film.component.FilmsRow
import com.flixclusive.feature.tv.film.component.buttons.MainButtons
import com.flixclusive.feature.tv.film.component.episodes.EpisodesPanel
import com.flixclusive.feature.tv.player.PlayerScreen
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun FilmScreen(
    navigator: CommonScreenNavigator
) {
    val viewModel = hiltViewModel<FilmScreenViewModel>()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
    val film by viewModel.film.collectAsStateWithLifecycle()
    val currentSeasonSelected by viewModel.currentSeasonSelected.collectAsStateWithLifecycle()

    var episodeToWatch: TMDBEpisode? by remember { mutableStateOf(null) }
    var anItemHasBeenClicked by remember { mutableStateOf(false) }

    var isOverviewShowing by remember { mutableStateOf(true) }
    var isPlayerRunning by remember { mutableStateOf(false) }
    var isEpisodesPanelOpen by remember { mutableStateOf(false) }
    val shouldNotFocusOnEpisodeButton = remember { mutableStateOf(true) }

    var buttonsHasFocus by remember { mutableStateOf(false) }
    var collectionHasFocus by remember { mutableStateOf(false) }
    var otherFilmsHasFocus by remember { mutableStateOf(false) }

    val delayPlayerAnimation = 1000
    val contentTransform = remember(isPlayerRunning) {
        ContentTransform(
            targetContentEnter = fadeIn() + slideInVertically(),
            initialContentExit = slideOutVertically(
                animationSpec = tween(
                    delayMillis = if(isPlayerRunning) delayPlayerAnimation else 0,
                    durationMillis = if(isPlayerRunning) delayPlayerAnimation else 300
                )
            ) + fadeOut(
                animationSpec = tween(
                    delayMillis = if(isPlayerRunning) delayPlayerAnimation else 0,
                    durationMillis = if(isPlayerRunning) delayPlayerAnimation else 300
                )
            ),
        )
    }
    val backdropPath = remember(film) {
        context.buildImageUrl(
            imagePath = film?.backdropImage,
            imageSize = "w1920_and_h600_multi_faces"
        )
    }
    val bottomFade = remember(buttonsHasFocus) {
        if(buttonsHasFocus) {
            Brush.verticalGradient(
                0.9F to Color.Red,
                1F to Color.Transparent
            )
        } else {
            Brush.verticalGradient(
                0F to Color.Transparent,
                0.1F to Color.Red,
                0.9F to Color.Red,
                1F to Color.Transparent
            )
        }
    }

    BackHandler(enabled = !isPlayerRunning) {
        navigator.goBack()
    }

    LaunchedEffect(isPlayerRunning) {
        anItemHasBeenClicked = false
    }

    //LaunchedEffect(Unit) {
    //    isPlayerStarting = false
    //    delay(8000L)
    //    isPlayerStarting = true
    //
    //    isOverviewShowing = false
    //    isBackdropImageShowing = false
    //    isEpisodesPanelOpen = false
    //}

    LaunchedEffect(Unit) {
        anItemHasBeenClicked = false
    }

    LocalFocusTransferredOnLaunchProvider {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = backdropPath,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(),
                        initialContentExit = fadeOut(
                            animationSpec = tween(
                                delayMillis = if(isPlayerRunning) 800 else 0,
                                durationMillis = if(isPlayerRunning) 800 else 300
                            )
                        )
                    )
                },
                label = "",
                modifier = Modifier.padding(start = useLocalDrawerWidth())
            ) {
                Box(
                    modifier = Modifier
                        .drawScrimOnBackground(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .height(400.dp),
                            model = it,
                            imageLoader = LocalContext.current.imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            TvLazyColumn(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fadingEdge(bottomFade),
                pivotOffsets = PivotOffsets(
                    parentFraction = if (collectionHasFocus || otherFilmsHasFocus) 0.4F else 0.8F
                ),
                contentPadding = PaddingValues(top = 35.dp, bottom = 35.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = isOverviewShowing && film != null,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = slideOutHorizontally() + fadeOut(),
                        label = "",
                        modifier = Modifier.padding(
                            start = useLocalDrawerWidth(),
                            bottom = 55.dp
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(35.dp),
                            modifier = Modifier.onFocusChanged {
                                buttonsHasFocus = it.hasFocus
                            }
                        ) {
                            FilmOverview(
                                film = film!!,
                                shouldEllipsize = false
                            )

                            MainButtons(
                                watchHistoryItem = watchHistoryItem,
                                isInWatchlist = uiState.isFilmInWatchlist,
                                isTvShow = film!!.filmType == FilmType.TV_SHOW,
                                shouldFocusOnPlayButton = !anItemHasBeenClicked && uiState.lastFocusedItem == null,
                                shouldFocusOnEpisodesButton = shouldNotFocusOnEpisodeButton,
                                onPlay = {
                                    isPlayerRunning = true
                                    isOverviewShowing = false
                                },
                                onWatchlistClick = viewModel::onWatchlistButtonClick,
                                onSeeMoreEpisodes = {
                                    isEpisodesPanelOpen = true
                                    isOverviewShowing = false
                                }
                            )
                        }
                    }
                }

                if(isOverviewShowing) {
                    if (film is Movie) {
                        item {
                            AnimatedContent(
                                targetState = film,
                                transitionSpec = { contentTransform },
                                label = ""
                            ) { film ->
                                (film as Movie).collection?.let { collection ->
                                    val rowIndex = 1
                                    Box(
                                        modifier = Modifier.padding(bottom = 25.dp)
                                    ) {
                                        FilmsRow(
                                            films = collection.films,
                                            hasFocus = collectionHasFocus,
                                            label = UiText.StringValue(collection.collectionName),
                                            iconId = UiCommonR.drawable.round_library,
                                            currentFilm = film,
                                            lastFocusedItem = uiState.lastFocusedItem,
                                            anItemHasBeenClicked = anItemHasBeenClicked,
                                            rowIndex = rowIndex,
                                            onFocusChange = {
                                                collectionHasFocus = it
                                            },
                                            onFilmClick = { columnIndex, film ->
                                                viewModel.onLastItemFocusChange(rowIndex, columnIndex)
                                                anItemHasBeenClicked = true
                                                navigator.openFilmScreen(film)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if(film?.recommendedTitles?.isNotEmpty() == true) {
                        item {
                            AnimatedContent(
                                targetState = film!!.recommendedTitles,
                                transitionSpec = { contentTransform },
                                label = ""
                            ) { films ->
                                val rowIndex = 2

                                FilmsRow(
                                    films = films,
                                    hasFocus = otherFilmsHasFocus,
                                    label = UiText.StringResource(UtilR.string.other_films_message),
                                    iconId = R.drawable.round_dashboard_24,
                                    currentFilm = film!!,
                                    lastFocusedItem = uiState.lastFocusedItem,
                                    anItemHasBeenClicked = anItemHasBeenClicked,
                                    rowIndex = rowIndex,
                                    onFocusChange = {
                                        otherFilmsHasFocus = it
                                    },
                                    onFilmClick = { columnIndex, film ->
                                        viewModel.onLastItemFocusChange(rowIndex, columnIndex)
                                        anItemHasBeenClicked = true
                                        navigator.openFilmScreen(film)
                                    }
                                )
                            }
                        }
                    }

                    items(10) {
                        NonFocusableSpacer(height = 50.dp)
                    }
                }
            }

            if(film is TvShow) {
                EpisodesPanel(
                    isVisible = isEpisodesPanelOpen,
                    film = film as TvShow,
                    currentSelectedSeasonNumber = viewModel.selectedSeasonNumber,
                    currentSelectedSeason = currentSeasonSelected,
                    onSeasonChange = {
                        if(it != viewModel.selectedSeasonNumber) {
                            viewModel.onSeasonChange(it)
                        }
                    },
                    onEpisodeClick = {
                        episodeToWatch = it
                        isPlayerRunning = true
                    },
                    onHidePanel = {
                        shouldNotFocusOnEpisodeButton.value = false
                        isEpisodesPanelOpen = false
                        isOverviewShowing = true
                    }
                )
            }

            AnimatedContent(
                targetState = film,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(),
                        initialContentExit = fadeOut()
                    )
                },
                label = "",
                modifier = Modifier.align(Alignment.Center)
            ) { item ->
                item?.let {
                    PlayerScreen(
                        args = PlayerScreenNavArgs(film = it, episodeToPlay = episodeToWatch),
                        isPlayerStarting = isPlayerRunning,
                        onBack = {
                            isPlayerRunning = false

                            if(!isEpisodesPanelOpen)
                                isOverviewShowing = true
                        }
                    )
                }
            }
        }
    }
}