package com.flixclusive.presentation.tv.screens.film

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
import androidx.media3.common.util.UnstableApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenNavArgs
import com.flixclusive.presentation.common.viewmodels.film.FilmScreenViewModel
import com.flixclusive.presentation.destinations.FilmTvScreenDestination
import com.flixclusive.presentation.tv.common.FilmTvOverview
import com.flixclusive.presentation.tv.common.TvRootNavGraph
import com.flixclusive.presentation.tv.main.InitialDrawerWidth
import com.flixclusive.presentation.tv.screens.player.FilmTvPlayerScreen
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.NonFocusableSpacer
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.drawScrimOnBackground
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@TvRootNavGraph
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = FadeInAndOutScreenTransition::class
)
@UnstableApi
@Composable
fun FilmTvScreen(navigator: DestinationsNavigator) {
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
        navigator.popBackStack()
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
            modifier = Modifier.padding(start = InitialDrawerWidth)
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
                        start = InitialDrawerWidth,
                        bottom = 55.dp
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(35.dp),
                        modifier = Modifier.onFocusChanged {
                            buttonsHasFocus = it.hasFocus
                        }
                    ) {
                        FilmTvOverview(
                            film = film!!,
                            shouldEllipsize = false
                        )

                        FilmTvButtons(
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
                                    FilmTvScreenRow(
                                        films = collection.films,
                                        hasFocus = collectionHasFocus,
                                        label = UiText.StringValue(collection.collectionName),
                                        iconId = R.drawable.round_library,
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
                                            navigator.navigate(
                                                FilmTvScreenDestination(film),
                                                onlyIfResumed = true
                                            )
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

                            FilmTvScreenRow(
                                films = films,
                                hasFocus = otherFilmsHasFocus,
                                label = UiText.StringResource(R.string.other_films_message),
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
                                    navigator.navigate(
                                        FilmTvScreenDestination(film),
                                        onlyIfResumed = true
                                    )
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
            FilmTvEpisodesPanel(
                isPlayerRunning = isPlayerRunning,
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
                FilmTvPlayerScreen(
                    film = it,
                    isPlayerStarting = isPlayerRunning,
                    episode = episodeToWatch,
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