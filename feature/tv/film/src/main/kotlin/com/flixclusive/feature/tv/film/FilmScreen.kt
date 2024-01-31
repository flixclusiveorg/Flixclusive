package com.flixclusive.feature.tv.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.core.ui.tv.FadeInAndOutScreenTransition
import com.flixclusive.core.ui.tv.component.FilmOverview
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.drawScrimOnBackground
import com.flixclusive.core.ui.tv.util.drawScrimOnForeground
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.useLocalFocusTransferredOnLaunch
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.feature.tv.film.component.FilmErrorSnackbar
import com.flixclusive.feature.tv.film.component.FilmsRow
import com.flixclusive.feature.tv.film.component.buttons.EPISODES_BUTTON_KEY
import com.flixclusive.feature.tv.film.component.buttons.MainButtons
import com.flixclusive.feature.tv.film.component.buttons.PLAY_BUTTON_KEY
import com.flixclusive.feature.tv.film.component.episodes.EpisodesPanel
import com.flixclusive.feature.tv.player.PlayerScreen
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

interface FilmScreenTvNavigator : GoBackAction {
    fun openFilmScreenSeamlessly(film: Film)
}

@OptIn(ExperimentalAnimationApi::class)
@Destination(
    navArgsDelegate = FilmScreenNavArgs::class,
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun FilmScreen(
    navigator: FilmScreenTvNavigator,
    args: FilmScreenNavArgs
) {
    val viewModel = hiltViewModel<FilmScreenViewModel>()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
    val film by viewModel.film.collectAsStateWithLifecycle()
    val currentSeasonSelected by viewModel.currentSeasonSelected.collectAsStateWithLifecycle()

    var episodeToPlay: TMDBEpisode? by remember { mutableStateOf(null) }

    var isPlayerRunning by remember { mutableStateOf(false) }
    var isEpisodesPanelOpen by remember { mutableStateOf(false) }
    var isOverviewShown by remember { mutableStateOf(true) }

    var buttonsHasFocus by remember { mutableStateOf(false) }
    var collectionHasFocus by remember { mutableStateOf(false) }
    var otherFilmsHasFocus by remember { mutableStateOf(false) }

    val delayPlayerAnimation = 1000

    val filmsRowEnterTransition = fadeIn() + slideInVertically()
    val filmsRowExitTransition = slideOutVertically(
        animationSpec = tween(
            delayMillis = if (isPlayerRunning) delayPlayerAnimation else 0,
            durationMillis = if (isPlayerRunning) delayPlayerAnimation else 300
        )
    ) + fadeOut(
        animationSpec = tween(
            delayMillis = if (isPlayerRunning) delayPlayerAnimation else 0,
            durationMillis = if (isPlayerRunning) delayPlayerAnimation else 300
        )
    )

    val backdropPath = remember(film) {
        context.buildImageUrl(
            imagePath = film?.backdropImage,
            imageSize = "w1920_and_h600_multi_faces"
        )
    }
    val bottomFade = remember(buttonsHasFocus) {
        if (buttonsHasFocus) {
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

    val lastItemFocusedMap = useLocalLastFocusedItemPerDestination()
    val currentRoute = useLocalCurrentRoute()

    //LaunchedEffect(Unit) {
    //    isPlayerStarting = false
    //    delay(8000L)
    //    isPlayerStarting = true
    //
    //    isOverviewShowing = false
    //    isBackdropImageShowing = false
    //    isEpisodesPanelOpen = false
    //}


    Box(
        modifier = Modifier
            .focusGroup()
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedContent(
            targetState = backdropPath,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(),
                    initialContentExit = fadeOut(
                        animationSpec = tween(
                            delayMillis = if (isPlayerRunning) 800 else 0,
                            durationMillis = if (isPlayerRunning) 800 else 300
                        )
                    )
                )
            },
            label = "",
            modifier = Modifier
                .padding(start = LabelStartPadding.start + useLocalDrawerWidth())
                .ifElse(
                    condition = !isOverviewShown,
                    ifTrueModifier = Modifier.drawScrimOnForeground()
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

        LocalFocusTransferredOnLaunchProvider {
            Box(
                modifier = Modifier
                    .focusGroup()
                    .fillMaxSize()
            ) {
                film?.let {
                    PlayerScreen(
                        film = it,
                        episodeToPlay = episodeToPlay,
                        isPlayerRunning = isPlayerRunning,
                        isOverviewShown = isOverviewShown,
                        onBack = { forceClose ->
                            if (isEpisodesPanelOpen || forceClose) {
                                isPlayerRunning = false
                            } else isOverviewShown = true
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isOverviewShown,
            enter = fadeIn(),
            exit = fadeOut(),
            label = "",
            modifier = Modifier
                .drawScrimOnBackground()
        ) {
            LocalFocusTransferredOnLaunchProvider {
                BackHandler {
                    navigator.goBack()
                }

                val isInitialLaunchTransferred = useLocalFocusTransferredOnLaunch()

                DisposableEffect(LocalLifecycleOwner.current) {
                    viewModel.initializeData(
                        filmId = args.film.id,
                        filmType = args.film.filmType
                    )

                    lastItemFocusedMap.getOrPut(currentRoute) {
                        PLAY_BUTTON_KEY
                    }

                    onDispose {
                        lastItemFocusedMap.remove(currentRoute)
                        isInitialLaunchTransferred.value = false
                    }
                }

                TvLazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fadingEdge(bottomFade),
                    pivotOffsets = PivotOffsets(
                        parentFraction = if (collectionHasFocus || otherFilmsHasFocus) 0.4F else 0.8F
                    ),
                    contentPadding = PaddingValues(top = 35.dp, bottom = 35.dp),
                ) {
                    item {
                        if (film != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(35.dp),
                                modifier = Modifier
                                    .focusGroup()
                                    .padding(
                                        start = LabelStartPadding.start + useLocalDrawerWidth(),
                                        bottom = 55.dp
                                    )
                                    .animateEnterExit(
                                        enter = slideInHorizontally(),
                                        exit = slideOutHorizontally()
                                    )
                                    .onFocusChanged { buttonsHasFocus = it.hasFocus }
                            ) {
                                AnimatedContent(
                                    targetState = film!!,
                                    transitionSpec = {
                                        ContentTransform(
                                            targetContentEnter = fadeIn(),
                                            initialContentExit = fadeOut(),
                                        )
                                    },
                                    label = ""
                                ) {
                                    FilmOverview(
                                        film = it,
                                        watchHistoryItem = watchHistoryItem,
                                        shouldEllipsize = false
                                    )
                                }

                                MainButtons(
                                    watchHistoryItem = watchHistoryItem,
                                    isInWatchlist = uiState.isFilmInWatchlist,
                                    isTvShow = film?.filmType == FilmType.TV_SHOW,
                                    onPlay = {
                                        isOverviewShown = false
                                        isPlayerRunning = true
                                    },
                                    onWatchlistClick = viewModel::onWatchlistButtonClick,
                                    goBack = navigator::goBack,
                                    onSeeMoreEpisodes = { isEpisodesPanelOpen = true }
                                )
                            }
                        }
                    }

                    if (film?.filmType == FilmType.MOVIE) {
                        item {
                            (film as Movie).collection?.let {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 25.dp)
                                        .animateEnterExit(
                                            enter = filmsRowEnterTransition,
                                            exit = filmsRowExitTransition
                                        )
                                ) {
                                    FilmsRow(
                                        films = it.films,
                                        hasFocus = collectionHasFocus,
                                        label = UiText.StringValue(it.collectionName),
                                        iconId = UiCommonR.drawable.round_library,
                                        currentFilm = film as Movie,
                                        goBack = navigator::goBack,
                                        onFocusChange = {
                                            collectionHasFocus = it
                                        },
                                        onFilmClick = { newFilm ->
                                            isPlayerRunning = false
                                            viewModel.initializeData(
                                                filmId = newFilm.id,
                                                filmType = newFilm.filmType
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (film?.recommendedTitles?.isNotEmpty() == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .animateEnterExit(
                                        enter = filmsRowEnterTransition,
                                        exit = filmsRowExitTransition
                                    )
                            ) {
                                FilmsRow(
                                    films = film!!.recommendedTitles,
                                    hasFocus = otherFilmsHasFocus,
                                    label = UiText.StringResource(UtilR.string.other_films_message),
                                    iconId = R.drawable.round_dashboard_24,
                                    currentFilm = film!!,
                                    goBack = navigator::goBack,
                                    onFocusChange = {
                                        otherFilmsHasFocus = it
                                    },
                                    onFilmClick = {
                                        isPlayerRunning = false
                                        navigator.openFilmScreenSeamlessly(it)
                                    }
                                )
                            }
                        }
                    }

                    items(2) {
                        NonFocusableSpacer(height = 50.dp)
                    }
                }
            }

            FilmErrorSnackbar(
                errorMessage = viewModel.errorSnackBarMessage,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }


    if (film is TvShow) {
        AnimatedVisibility(
            visible = isEpisodesPanelOpen && !isPlayerRunning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LocalFocusTransferredOnLaunchProvider {
                Box(
                    modifier = Modifier
                        .focusGroup()
                        .fillMaxSize()
                ) {
                    EpisodesPanel(
                        film = film as TvShow,
                        currentSelectedSeasonNumber = viewModel.selectedSeasonNumber,
                        currentSelectedSeason = currentSeasonSelected,
                        onSeasonChange = viewModel::onSeasonChange,
                        onEpisodeClick = {
                            episodeToPlay = it
                            isPlayerRunning = true
                        },
                        onHidePanel = {
                            isEpisodesPanelOpen = false

                            // Focus on episode button.
                            lastItemFocusedMap[currentRoute] = EPISODES_BUTTON_KEY
                        }
                    )
                }
            }
        }
    }
}