package com.flixclusive.mobile.component

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.mobile.extensions.toggleSystemBars
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import kotlinx.coroutines.delay

@Composable
internal fun PlayerSplashScreen(
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.getActivity<ComponentActivity>()

    BackHandler {
        // No-op to disable back navigation while on the splash screen
    }

    LaunchedEffect(Unit) {
        delay(200)
        activity.toggleSystemBars(isVisible = false)
        delay(600L)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize()
    )
}

@Preview
@Composable
private fun PlayerSplashScreenBasePreview() {
    FlixclusiveTheme {
        Surface {

        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlayerSplashScreenCompactLandscapePreview() {
    PlayerSplashScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PlayerSplashScreenMediumPortraitPreview() {
    PlayerSplashScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PlayerSplashScreenMediumLandscapePreview() {
    PlayerSplashScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PlayerSplashScreenExtendedPortraitPreview() {
    PlayerSplashScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PlayerSplashScreenExtendedLandscapePreview() {
    PlayerSplashScreenBasePreview()
}
