package com.flixclusive.feature.mobile.player.component.effect

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flixclusive.core.presentation.common.extensions.getActivity

@Composable
internal fun ToggleSystemBarsEffect() {
    val context = LocalContext.current.getActivity<Activity>()

    DisposableEffect(LocalLifecycleOwner.current) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        context.toggleSystemBars(isVisible = false)

        onDispose {
            // TODO: Watch out orientation changes when user selects a different episode/season/provider,
            //  maybe we should only reset orientation when user leaves the player screen
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            context.toggleSystemBars(isVisible = true)
        }
    }
}

@Suppress("DEPRECATION")
internal fun Activity.toggleSystemBars(isVisible: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (isVisible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.ime())
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        return
    }

    val state = if(!isVisible) {
        (window.decorView.systemUiVisibility
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
    } else (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    if (window.decorView.systemUiVisibility != state) {
        window.decorView.systemUiVisibility = state
    }
}
