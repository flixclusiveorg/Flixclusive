package com.flixclusive.feature.mobile.player

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.mobile.extensions.toggleSystemBars
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

interface NavigatorPlayerSplashScreen {
    fun navigateToPlayerScreen(
        media: MediaMetadata,
        episode: Episode?,
        initialStreamUrl: String?,
        initialCacheId: String?,
        initialHeaders: Map<String, String>?
    )
}

@Destination<ExternalModuleGraph>(
    style = PlayerSplashScreenTransitionStyle::class,
    navArgs = PlayerScreenNavArgs::class
)
@Composable
internal fun PlayerSplashScreen(
    args: PlayerScreenNavArgs,
    navigator: NavigatorPlayerSplashScreen,
) {
    val activity = LocalContext.current.getActivity<ComponentActivity>()

    BackHandler {
        // No-op to disable back navigation while on the splash screen
    }

    LaunchedEffect(Unit) {
        delay(700L.milliseconds)
        activity.toggleSystemBars(isVisible = false)
        delay(600L.milliseconds)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        delay(600L.milliseconds)
        navigator.navigateToPlayerScreen(
            media = args.media,
            episode = args.episode,
            initialStreamUrl = args.initialStreamUrl,
            initialCacheId = args.initialCacheId,
            initialHeaders = args.initialHeaders?.headers
        )
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    )
}

internal object PlayerSplashScreenTransitionStyle : DestinationStyle.Animated() {
    override val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)
        get() = {
            slideInHorizontally(
                tween(
                    durationMillis = 450,
                    delayMillis = 500
                )
            ) { it }
        }

    override val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)
        get() = { slideOutHorizontally(tween(450)) { -it } }
}
