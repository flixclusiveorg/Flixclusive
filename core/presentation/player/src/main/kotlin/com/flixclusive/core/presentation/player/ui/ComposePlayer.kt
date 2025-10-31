package com.flixclusive.core.presentation.player.ui

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import com.flixclusive.core.presentation.player.AppDataSourceFactoryImpl
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayerImpl
import com.flixclusive.core.presentation.player.PlayerCache
import com.flixclusive.core.presentation.player.extensions.toContentScale
import com.flixclusive.core.presentation.player.ui.state.ControlsVisibilityState.Companion.rememberControlsVisibilityState
import com.google.common.collect.ImmutableList
import okhttp3.OkHttpClient

/**
 * A composable that displays a video player with subtitles and handles the player's lifecycle.
 * */

@OptIn(UnstableApi::class)
@Composable
fun ComposePlayer(
    player: AppPlayer,
    resizeMode: ResizeMode,
    modifier: Modifier = Modifier,
) {
    val presentationState = rememberPresentationState(player)

    Box(modifier = modifier) {
        PlayerSurface(
            player = player,
            modifier = Modifier.resizeWithContentScale(
                contentScale = resizeMode.toContentScale(),
                sourceSizeDp = presentationState.videoSizeDp,
            ),
        )

        AndroidView(
            factory = ::SubtitleView,
            update = { player.subtitleView = it },
            modifier = Modifier.fillMaxSize(),
        )

        if (presentationState.coverSurface) {
            // Cover the surface that is being prepared with a shutter
            // Do not use scaledModifier here, makes the Box be measured at 0x0
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black),
            )
        }
    }

    if (Build.VERSION.SDK_INT > 23) {
        // Initialize/release in onStart()/onStop() only because in a multi-window environment multiple
        // apps can be visible at the same time. The apps that are out-of-focus are paused, but video
        // playback should continue.
        LifecycleStartEffect(Unit) {
            player.initialize()
            if (player.playWhenReady) {
                player.play()
            }

            onStopOrDispose {
                player.playWhenReady = player.isPlaying
                player.pause()
                player.release()
            }
        }
    } else {
        // Call to onStop() is not guaranteed, hence we release the Player in onPause() instead
        LifecycleResumeEffect(Unit) {
            player.initialize()
            if (player.playWhenReady) {
                player.play()
            }

            onPauseOrDispose {
                player.release()
            }
        }
    }
}

@Preview
@Composable
private fun ComposePlayerPreview() {
    val context = LocalContext.current

    val player = remember {
        AppPlayerImpl(
            context = context,
            subtitlePrefs = SubtitlesPreferences(),
            playerPrefs = PlayerPreferences(resizeMode = ResizeMode.Fit),
            dataSourceFactory = AppDataSourceFactoryImpl(
                context = context,
                client = OkHttpClient(),
                cache = PlayerCache(context).get(size = -1L),
            ),
        ).also {
            val videos = listOf(
                "https://html5demos.com/assets/dizzy.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-0/shortform_2.mp4",
                "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-vp9-360.webm",
                "https://storage.googleapis.com/exoplayer-test-media-0/shortform_3.mp4",
            )

            val subtitle =
                "https://gist.githubusercontent.com/matibzurovski/d690d5c14acbaa399e7f0829f9d6888e/raw/63578ca30e7430be1fa4942d4d8dd599f78151c7/example.srt"

            it.initialize()
            it.setMediaItems(
                videos.map { videoUrl ->
                    MediaItem
                        .Builder()
                        .setUri(videoUrl.toUri())
                        .setSubtitleConfigurations(
                            ImmutableList.of(
                                MediaItem.SubtitleConfiguration
                                    .Builder(subtitle.toUri())
                                    .apply {
                                        setMimeType("application/x-subrip")
                                        setLanguage("en")
                                        setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    }.build(),
                            ),
                        ).build()
                },
            )
            it.prepare()
            it.playWhenReady = true
        }
    }

    val controlsVisibilityState = rememberControlsVisibilityState(
        player,
        isScrubbing = false,
    )

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box {
                ComposePlayer(
                    player = player,
                    resizeMode = ResizeMode.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { controlsVisibilityState.toggle() },
                )

                if (controlsVisibilityState.isVisible) {
                    Button(
                        onClick = controlsVisibilityState::toggle,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                    ) {
                        Text("Show controls")
                    }
                }
            }
        }
    }
}
