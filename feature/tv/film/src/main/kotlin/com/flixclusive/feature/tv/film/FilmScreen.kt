package com.flixclusive.feature.tv.media

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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.util.CoilUtil.buildImageUrl
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.media.MediaScreenNavArgs
import com.flixclusive.core.ui.tv.FadeInAndOutScreenTransition
import com.flixclusive.core.ui.tv.component.MediaOverview
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.drawScrimOnBackground
import com.flixclusive.core.ui.tv.util.drawScrimOnForeground
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalFocusTransferredOnLaunch
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.feature.tv.media.component.MediaErrorSnackbar
import com.flixclusive.feature.tv.media.component.MediasRow
import com.flixclusive.feature.tv.media.component.buttons.EPISODES_BUTTON_KEY
import com.flixclusive.feature.tv.media.component.buttons.MainButtons
import com.flixclusive.feature.tv.media.component.buttons.PLAY_BUTTON_KEY
import com.flixclusive.feature.tv.media.component.episodes.EpisodesPanel
import com.flixclusive.feature.tv.player.PlayerScreen
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface MediaScreenTvNavigator : GoBackAction {
    fun openMediaScreenSeamlessly(media: MediaMetadata)
}

@Destination<ExternalModuleGraph>(
    navArgs = MediaScreenNavArgs::class,
    style = FadeInAndOutScreenTransition::class
)
@Composable
internal fun MediaScreen(
    navigator: MediaScreenTvNavigator,
    args: MediaScreenNavArgs
,viewModel: MediaScreenViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val currentSeasonSelected by viewModel.currentSeasonSelected.collectAsStateWithLifecycle()

    var episodeToPlay: Episode? by remember { mutableStateOf(null) }

    var isPlayerRunning by remember { mutableStateOf(args.startPlayerAutomatically) }
    var isEpisodesPanelOpen by remember { mutableStateOf(false) }
    var isOverviewShown by remember { mutableStateOf(!args.startPlayerAutomatically) }
    var isIdle by remember { mutableStateOf(true) }

    var buttonsHasFocus by remember { mutableStateOf(false) }
    var collectionHasFocus by remember { mutableStateOf(false) }
    var otherMediasHasFocus by remember { mutableStateOf(false) }

    val delayPlayerAnimation = 1000

    val mediasRowEnterTransition = fadeIn() + slideInVertically()
    val mediasRowExitTransition = slideOutVertically(
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

    val backdropPath = remember(media) {
        context.buildTMDBImageUrl(imagePath = media?.backdropImage)
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

    Box(
        modifier = Modifier
            .focusGroup()
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    isIdle = false
                }

                false
            }
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
                .padding(start = LabelStartPadding.start + getLocalDrawerWidth())
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
                    contentDescription = stringResource(id = LocaleR.string.media_item_content_description)
                )
            }
        }

        LocalFocusTransferredOnLaunchProvider {
            Box(
                modifier = Modifier
                    .focusGroup()
                    .fillMaxSize()
            ) {
                media?.let {
                    PlayerScreen(
                        media = it,
                        episodeToPlay = episodeToPlay,
                        isPlayerRunning = isPlayerRunning,
                        isOverviewShown = isOverviewShown,
                        isIdle = isIdle,
                        onPlayerScreenVisibilityChange = { visible ->
                            isPlayerRunning = visible
                        },
                        onOverviewVisibilityChange = { visible ->
                            isOverviewShown = visible
                        },
                        onBack = { forceClose ->
                            if (isEpisodesPanelOpen || forceClose) {
                                isPlayerRunning = false
                            }

                            isOverviewShown = true
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
                    isIdle = true
                    viewModel.initializeData()

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
                        parentFraction = if (collectionHasFocus || otherMediasHasFocus) 0.4F else 0.8F
                    ),
                    contentPadding = PaddingValues(top = 35.dp, bottom = 35.dp),
                ) {
                    item {
                        if (media != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(35.dp),
                                modifier = Modifier
                                    .focusGroup()
                                    .padding(
                                        start = LabelStartPadding.start + getLocalDrawerWidth(),
                                        bottom = 55.dp
                                    ).animateEnterExit(
                                        enter = slideInHorizontally(),
                                        exit = slideOutHorizontally()
                                    ).onFocusChanged { buttonsHasFocus = it.hasFocus }
                            ) {
                                AnimatedContent(
                                    targetState = media!!,
                                    transitionSpec = {
                                        ContentTransform(
                                            targetContentEnter = fadeIn(),
                                            initialContentExit = fadeOut(),
                                        )
                                    },
                                    label = ""
                                ) {
                                    MediaOverview(
                                        media = it,
                                        watchHistoryItem = watchHistoryItem,
                                        shouldEllipsize = false
                                    )
                                }

                                MainButtons(
                                    watchHistoryItem = watchHistoryItem,
                                    isInWatchlist = uiState.isMediaInWatchlist,
                                    isShow = media?.type == MediaType.SHOW,
                                    onPlay = {
                                        isOverviewShown = false
                                        isPlayerRunning = true
                                    },
                                    onWatchlistClick = viewModel::toggleAsWatchList,
                                    goBack = navigator::goBack,
                                    onSeeMoreEpisodes = {
                                        isOverviewShown = false
                                        isEpisodesPanelOpen = true
                                    }
                                )
                            }
                        }
                    }

                    if (media?.type == MediaType.MOVIE) {
                        item {
                            (media as Movie).collection?.let {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 25.dp)
                                        .animateEnterExit(
                                            enter = mediasRowEnterTransition,
                                            exit = mediasRowExitTransition
                                        )
                                ) {
                                    MediasRow(
                                        medias = it.medias,
                                        hasFocus = collectionHasFocus,
                                        label = UiText.StringValue(it.collectionName),
                                        iconId = UiCommonR.drawable.round_library,
                                        currentMedia = media as Movie,
                                        goBack = navigator::goBack,
                                        onFocusChange = {
                                            collectionHasFocus = it
                                        },
                                        onMediaClick = { newMedia ->
                                            isPlayerRunning = false
                                            viewModel.initializeData(media = newMedia)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (media?.recommendations?.isNotEmpty() == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .animateEnterExit(
                                        enter = mediasRowEnterTransition,
                                        exit = mediasRowExitTransition
                                    )
                            ) {
                                MediasRow(
                                    medias = media!!.recommendations,
                                    hasFocus = otherMediasHasFocus,
                                    label = UiText.StringResource(LocaleR.string.other_medias_message),
                                    iconId = R.drawable.round_dashboard_24,
                                    currentMedia = media!!,
                                    goBack = navigator::goBack,
                                    onFocusChange = {
                                        otherMediasHasFocus = it
                                    },
                                    onMediaClick = {
                                        isPlayerRunning = false
                                        navigator.openMediaScreenSeamlessly(it)
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

            MediaErrorSnackbar(
                errorMessage = viewModel.errorSnackBarMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }
    }

    if (media is Show) {
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
                        media = media as Show,
                        currentSelectedSeasonNumber = viewModel.selectedSeasonNumber,
                        currentSelectedSeason = currentSeasonSelected,
                        onSeasonChange = viewModel::onSeasonChange,
                        onEpisodeClick = {
                            episodeToPlay = it
                            isPlayerRunning = true
                            isOverviewShown = false
                        },
                        onHidePanel = {
                            isOverviewShown = true
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
