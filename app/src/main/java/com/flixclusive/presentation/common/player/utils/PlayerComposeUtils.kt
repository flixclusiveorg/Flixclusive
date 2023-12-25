package com.flixclusive.presentation.common.player.utils

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.flixclusive.R
import com.flixclusive.databinding.CustomPlayerBinding
import com.flixclusive.presentation.common.player.FlixclusivePlayer

object PlayerComposeUtils {

    val LocalPlayer = compositionLocalOf<FlixclusivePlayer?> { null }

    @Composable
    fun rememberLocalPlayer(): FlixclusivePlayer {
        val player = LocalPlayer.current
        check(player != null) { "Player has not been initialized" }

        return rememberUpdatedState(player).value
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("OpaqueUnitKey")
    @Composable
    fun LifecycleAwarePlayer(
        modifier: Modifier = Modifier,
        isInPipModeProvider: () -> Boolean = { false },
        isInTv: Boolean = false,
        releaseOnStop: Boolean = true,
        resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        areControlsVisible: Boolean,
        onInitialize: () -> Unit,
        onRelease: () -> Unit,
    ) {
        val lifecycle by rememberUpdatedState(LocalLifecycleOwner.current.lifecycle)
        val player = rememberLocalPlayer()

        val isInPipMode by rememberUpdatedState(isInPipModeProvider())

        DisposableEffect(
            AndroidViewBinding(
                modifier = modifier,
                factory = { inflater, viewGroup, attachToParent ->
                    val binding = CustomPlayerBinding.inflate(inflater, viewGroup, attachToParent)

                    binding.apply {
                        val subtitleViewHolder = root.findViewById<FrameLayout?>(R.id.subtitle_view_holder)

                        root.isClickable = false
                        root.isFocusable = false

                        playerView.run {
                            this@run.resizeMode = resizeMode
                            this@run.player = player.getPlayer()

                            // Show the controls forever
                            controllerShowTimeoutMs = Int.MAX_VALUE
                            showController()

                            // Move out the SubtitleView ouf ot the content frame
                            // To avoid zooming in when resizeMode == CENTER_CROP
                            (subtitleView?.parent as ViewGroup?)?.removeView(subtitleView)
                            subtitleViewHolder?.addView(subtitleView)
                        }
                    }

                    return@AndroidViewBinding binding
                }
            ) {
                playerView.run {
                    this.resizeMode = resizeMode
                    this.player = player.getPlayer()

                    if(!isControllerFullyVisible) {
                        showController()
                    }

                    player.setSubtitleStyle(
                        subtitleView = subtitleView,
                        isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPipMode,
                        isInTv = isInTv,
                        areControlsVisible = areControlsVisible
                    )
                }
            }
        ) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (player.getPlayer() == null) {
                            onInitialize()
                        } else if (player.playWhenReady) {
                            player.play()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        player.playWhenReady = player.isPlaying
                        player.pause()

                        if (Build.VERSION.SDK_INT > 23 && releaseOnStop) {
                            onRelease()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (Build.VERSION.SDK_INT <= 23 && releaseOnStop) {
                            onRelease()
                        }
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        if (!releaseOnStop) {
                            onRelease()
                        }
                    }

                    else -> Unit
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                if (isInTv) {
                    onRelease()
                }
                lifecycle.removeObserver(observer)
            }
        }
    }
}