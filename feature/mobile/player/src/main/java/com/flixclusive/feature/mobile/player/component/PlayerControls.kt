package com.flixclusive.feature.mobile.player.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.ControlsVisibilityState.Companion.rememberControlsVisibilityState
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState.Companion.rememberPlayPauseButtonState
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState.Companion.rememberPlaybackSpeedState
import com.flixclusive.core.presentation.player.ui.state.ScrubEvent
import com.flixclusive.core.presentation.player.ui.state.ScrubState.Companion.rememberScrubState
import com.flixclusive.core.presentation.player.ui.state.SeekButtonState.Companion.rememberSeekButtonState
import com.flixclusive.feature.mobile.player.component.bottom.BottomControls
import com.flixclusive.feature.mobile.player.component.bottom.PlaybackSpeedSheet
import com.flixclusive.feature.mobile.player.component.top.PlayerTopBar
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode

@Composable
internal fun PlayerControls(
    player: AppPlayer,
    film: FilmMetadata,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    episode: Episode? = null,
    onNext: (() -> Unit)? = null
) {
    var isSpeedPanelOpened by rememberSaveable { mutableStateOf(false) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var isCcPanelOpened by rememberSaveable { mutableStateOf(false) }
    var isServersPanelOpened by rememberSaveable { mutableStateOf(false) }
    var isSubsSyncPanelOpened by rememberSaveable { mutableStateOf(false) }

    val scrubState = rememberScrubState(player = player)
    val playPauseState = rememberPlayPauseButtonState(player = player)
    val seekButtonState = rememberSeekButtonState(player = player)
    val playbackSpeedState = rememberPlaybackSpeedState(player = player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        isScrubbing = scrubState.event == ScrubEvent.SCRUBBING
    )

    AnimatedContent(
        targetState = isLocked,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut()
            )
        }
    ) { state ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .noIndicationClickable(
                    onClick = {
                        if (isSpeedPanelOpened) {
                            isSpeedPanelOpened = false
                        } else {
                            controlsVisibilityState.toggle()
                        }
                    }
                )
        ) {
            if (state) {
                AnimatedVisibility(
                    visible = controlsVisibilityState.isVisible,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(durationMillis = 400))
                ) {
                    LockControls(
                        unlock = { isLocked = false },
                        showControls = { controlsVisibilityState.show() }
                    )
                }
            } else {
                ControlsBlackOverlay(
                    visible = controlsVisibilityState.isVisible,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = false,
                    ) {
                        PlayerTopBar(
                            title = film.title,
                            episode = episode,
                            onBack = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    VerticalSlideAnimation(
                        visible = controlsVisibilityState.isVisible,
                        slideDown = true,
                    ) {
                        BottomControls(
                            playPauseState = playPauseState,
                            seekButtonState = seekButtonState,
                            playbackSpeedState = playbackSpeedState,
                            scrubState = scrubState,
                            isSpeedPanelOpen = isSpeedPanelOpened,
                            onToggleSpeedPanel = { isOpen -> isSpeedPanelOpened = isOpen },
                            onNext = onNext,
                            onLock = { isLocked = true },
                            onShowCcPanel = { isCcPanelOpened = true },
                            onShowServersPanel = { isServersPanelOpened = true },
                            onShowSubtitleSyncPanel = { isSubsSyncPanelOpened = true },
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun ControlsBlackOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        VerticalSlideAnimation(
            visible = visible,
            slideDown = false,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black.copy(0.8f),
                                0.15f to Color.Transparent,
                            )
                        )
                    }
            )
        }

        VerticalSlideAnimation(
            visible = visible,
            slideDown = true,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.75f to Color.Transparent,
                                1f to Color.Black.copy(0.8f),
                            )
                        )
                    }
            )
        }
    }
}

@Composable
private fun VerticalSlideAnimation(
    visible: Boolean,
    slideDown: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { (it / 4) * (if (slideDown) 1 else -1) },
        exit = fadeOut() + slideOutVertically { (it / 6) * (if (slideDown) 1 else -1)},
        content = content,
        modifier = modifier,
    )
}
