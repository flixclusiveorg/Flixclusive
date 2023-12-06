package com.flixclusive.presentation.mobile.screens.player.controls.gestures

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.mobile.screens.player.controls.SEEK_ANIMATION_DELAY
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSeeker(
    modifier: Modifier = Modifier,
    areControlsVisible: Boolean,
    @DrawableRes iconId: Int,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    seekAction: () -> Unit,
    showControls: (Boolean) -> Unit,
) {
    val isVisible by rememberUpdatedState(areControlsVisible)
    val scope = rememberCoroutineScope()

    var isSeeking by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val screenWidth = LocalConfiguration.current.screenWidthDp

    Box(
        modifier = modifier
            .fillMaxWidth(0.45F)
            .fillMaxHeight()
            .indication(
                interactionSource,
                rememberRipple(bounded = false, radius = screenWidth.dp.div(2F))
            )
            .pointerInput(LocalPlayer.current) {
                detectTapGestures(
                    onTap = {
                        showControls(!isVisible)
                    },
                    onDoubleTap = { offset ->
                        scope.launch {
                            var shouldShowControls = false
                            if (isVisible) {
                                showControls(false)
                                shouldShowControls = true
                            }

                            val press = PressInteraction.Press(offset)

                            isSeeking = true
                            interactionSource.emit(press)

                            seekAction()

                            interactionSource.emit(
                                PressInteraction.Release(
                                    press
                                )
                            )
                            delay(SEEK_ANIMATION_DELAY)
                            isSeeking = false

                            showControls(shouldShowControls)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSeeking,
            enter = enterTransition,
            exit = exitTransition
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
            )
        }
    }
}