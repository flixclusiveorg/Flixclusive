package com.flixclusive.presentation.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.consumet.Subtitle
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.player.PlayerSnackbarMessageType
import com.flixclusive.presentation.player.controls.episodes_sheet.MoreEpisodesSheet
import com.flixclusive.presentation.player.controls.qualities_and_subtitles_sheet.QualitiesAndSubtitlesSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5000L
private const val SEEK_ANIMATION_DELAY = 450L

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    watchHistoryItem: WatchHistoryItem?,
    seasonDataProvider: () -> Resource<Season>?,
    availableSeasons: Int?,
    currentSeasonSelected: Int?,
    currentEpisodeSelected: TMDBEpisode?,
    isLastEpisode: Boolean,
    subtitlesProvider: () -> List<Subtitle>,
    videoQualitiesProvider: () -> List<String>,
    shouldShowControls: () -> Boolean,
    showControls: (Boolean) -> Unit,
    onBack: () -> Unit,
    isPlaying: () -> Boolean,
    title: () -> String,
    onReplayClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPauseToggle: () -> Unit,
    totalDuration: () -> Long,
    currentTime: () -> Long,
    bufferedPercentage: () -> Int,
    playbackState: () -> Int,
    selectedSubtitleProvider: () -> Int,
    selectedVideoQualityProvider: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSnackbarToggle: (String, PlayerSnackbarMessageType) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onSubtitleChange: (Int) -> Unit,
    onVideoQualityChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode?) -> Unit,
) {
    val context = LocalContext.current

    val seasonData = remember(seasonDataProvider()) { seasonDataProvider() }

    val subtitles = remember(subtitlesProvider()) { subtitlesProvider() }
    val videoQualities = remember(videoQualitiesProvider()) { videoQualitiesProvider() }

    val isVisible = remember(shouldShowControls()) { shouldShowControls() }

    var controlsVisibilityTimeout by remember {
        mutableStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)
    }

    fun resetPlayerControlVisibility() {
        controlsVisibilityTimeout = PLAYER_CONTROL_VISIBILITY_TIMEOUT
    }

    val scope = rememberCoroutineScope()

    var shouldOpenEpisodesSheet by remember { mutableStateOf(false) }
    var shouldLockControls by remember { mutableStateOf(false) }
    var shouldOpenQualitiesAndSubtitles by remember { mutableStateOf(false) }

    val selectedSubtitle = remember(selectedSubtitleProvider()) { selectedSubtitleProvider() }
    val selectedVideoQuality = remember(selectedVideoQualityProvider()) { selectedVideoQualityProvider() }

    val topFadeEdge = remember { Brush.verticalGradient(0F to Color.Black, 0.9F to Color.Transparent) }
    val bottomFadeEdge = remember { Brush.verticalGradient(0F to Color.Transparent, 0.9F to Color.Black) }

    var isSeekingBack by remember { mutableStateOf(false) }
    var isSeekingForward by remember { mutableStateOf(false) }
    val interactionSourceLeft = remember { MutableInteractionSource() }
    val interactionSourceRight = remember { MutableInteractionSource() }
    val screenWidth = LocalConfiguration.current.screenWidthDp

    BackHandler {
        if(shouldOpenEpisodesSheet) {
            shouldOpenEpisodesSheet = false
            return@BackHandler
        }

        if(shouldOpenQualitiesAndSubtitles) {
            shouldOpenQualitiesAndSubtitles = false
            return@BackHandler
        }

        if(shouldLockControls) {
            resetPlayerControlVisibility()
            return@BackHandler
        }

        onBack()
    }

    LaunchedEffect(
        isVisible,
        shouldOpenQualitiesAndSubtitles,
        shouldOpenEpisodesSheet,
        controlsVisibilityTimeout
    ) {
        if(shouldOpenQualitiesAndSubtitles || shouldOpenEpisodesSheet) {
            return@LaunchedEffect
        }

        if(isVisible) {
            resetPlayerControlVisibility()
            var timeout = controlsVisibilityTimeout
            while (timeout > 0L) {
                delay(1000L)
                timeout -= 1000L
            }
            showControls(false)
        }
    }

    AnimatedVisibility(
        visible = isVisible && shouldLockControls,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(0.3F))
                }
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 45.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                onClick = {
                    shouldLockControls = false
                    resetPlayerControlVisibility()
                    showControls(true)
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.round_lock_open_24),
                        contentDescription = stringResource(R.string.unlock_content_description),
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(bottom = 10.dp)
                    )

                    Text(
                        text = stringResource(R.string.unlock_label),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if(!shouldLockControls) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Left clicker
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45F)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .indication(
                        interactionSourceLeft,
                        rememberRipple(bounded = false, radius = screenWidth.dp.div(2F))
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls(!shouldShowControls())
                            },
                            onDoubleTap = { offset ->
                                scope.launch {
                                    var shouldHide = false
                                    if (shouldShowControls()) {
                                        showControls(false)
                                        shouldHide = true
                                    }

                                    val press = PressInteraction.Press(offset)

                                    isSeekingBack = true
                                    interactionSourceLeft.emit(press)

                                    onSeekBack()

                                    interactionSourceLeft.emit(
                                        PressInteraction.Release(
                                            press
                                        )
                                    )
                                    delay(SEEK_ANIMATION_DELAY)
                                    isSeekingBack = false

                                    showControls(shouldHide)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSeekingBack,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 500)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_keyboard_double_arrow_left_24),
                        contentDescription = "Rewind icon",
                        modifier = Modifier
                            .size(52.dp)
                    )
                }
            }

            // Right clicker
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45F)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .indication(
                        interactionSourceRight,
                        rememberRipple(bounded = false, radius = screenWidth.dp.div(2F))
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls(!shouldShowControls())
                            },
                            onDoubleTap = { offset ->
                                var shouldHide = false
                                if (shouldShowControls()) {
                                    showControls(false)
                                    shouldHide = true
                                }

                                val press = PressInteraction.Press(offset)

                                scope.launch {
                                    isSeekingForward = true
                                    interactionSourceRight.emit(press)
                                }

                                onSeekForward()

                                scope.launch {
                                    interactionSourceRight.emit(PressInteraction.Release(press))
                                    delay(SEEK_ANIMATION_DELAY)
                                    isSeekingForward = false
                                    showControls(shouldHide)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSeekingForward,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 500)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_keyboard_double_arrow_right_24),
                        contentDescription = "Forward icon",
                        modifier = Modifier
                            .size(52.dp)
                    )
                }
            }


            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    TopControls(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .drawBehind {
                                drawRect(brush = topFadeEdge)
                            }
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int ->
                                        -fullHeight
                                    }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int ->
                                        -fullHeight
                                    }
                                )
                            ),
                        title = title,
                        onNavigationIconClick = onBack
                    )

                    CenterControls(
                        modifier = Modifier.align(Alignment.Center),
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        onReplayClick = {
                            onReplayClick()
                            resetPlayerControlVisibility()
                            showControls(true)
                        },
                        onForwardClick = {
                            onForwardClick()
                            resetPlayerControlVisibility()
                            showControls(true)
                        },
                        onPauseToggle = {
                            onPauseToggle()
                            resetPlayerControlVisibility()
                            showControls(true)
                        },
                    )

                    BottomControls(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .drawBehind {
                                drawRect(brush = bottomFadeEdge)
                            }
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int ->
                                        fullHeight
                                    }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int ->
                                        fullHeight
                                    }
                                )
                            ),
                        isTvShow = currentEpisodeSelected != null,
                        isLastEpisode = isLastEpisode,
                        totalDuration = totalDuration,
                        currentTime = currentTime,
                        bufferedPercentage = bufferedPercentage,
                        onSeekChanged = {
                            onSeekChanged(it)
                            resetPlayerControlVisibility()
                            showControls(true)
                        },
                        onMoreVideosClick = {
                            shouldOpenEpisodesSheet = true
                            showControls(false)
                        },
                        onNextEpisodeClick = onEpisodeClick,
                        onLockClick = {
                            shouldLockControls = true
                            resetPlayerControlVisibility()
                            showControls(true)
                        },
                        onQualityAndSubtitleClick = {
                            shouldOpenQualitiesAndSubtitles = true
                            showControls(false)
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = shouldOpenEpisodesSheet,
        enter = slideInHorizontally(animationSpec = tween(durationMillis = 500)),
        exit = slideOutHorizontally(animationSpec = tween(durationMillis = 500))
    ) {
        MoreEpisodesSheet(
            seasonData = seasonData!!,
            availableSeasons = availableSeasons!!,
            currentSeasonSelected = currentSeasonSelected!!,
            currentEpisodeSelected = currentEpisodeSelected!!,
            watchHistoryItem = watchHistoryItem,
            onEpisodeClick = {
                onEpisodeClick(it)
            },
            onSeasonChange = onSeasonChange,
            onDismissSheet = {
                shouldOpenEpisodesSheet = false
                resetPlayerControlVisibility()
                showControls(true)
            },
        )
    }

    AnimatedVisibility(
        visible = shouldOpenQualitiesAndSubtitles,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 500)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 500)
        )
    ) {
        QualitiesAndSubtitlesSheet(
            subtitles = subtitles,
            qualities = videoQualities,
            selectedSubtitle = selectedSubtitle,
            selectedQuality = selectedVideoQuality,
            onSubtitleChange = { i, message ->
                onSubtitleChange(i)

                val subtitleMessageFormat = UiText.StringResource(R.string.subtitle_snackbar_message).asString(context)
                onSnackbarToggle(String.format(subtitleMessageFormat, message), PlayerSnackbarMessageType.Subtitle)
            },
            onVideoQualityChange = { i, message ->
                onVideoQualityChange(i)

                val qualityMessageFormat = UiText.StringResource(R.string.quality_snackbar_message).asString(context)
                onSnackbarToggle(String.format(qualityMessageFormat, message), PlayerSnackbarMessageType.Quality)
            },
            onDismissSheet = {
                shouldOpenQualitiesAndSubtitles = false
                resetPlayerControlVisibility()
            }
        )
    }
}