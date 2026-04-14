package com.flixclusive.feature.mobile.player.component.effect

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flixclusive.core.presentation.common.extensions.getActivity

@Composable
internal fun ToggleOrientationEffect() {
    val context = LocalContext.current.getActivity<Activity>()

    DisposableEffect(LocalLifecycleOwner.current) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            // TODO: Watch out orientation changes when user selects a different episode/season/provider,
            //  maybe we should only reset orientation when user leaves the player screen
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
