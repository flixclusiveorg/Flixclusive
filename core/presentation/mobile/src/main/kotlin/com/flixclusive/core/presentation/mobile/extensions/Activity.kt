package com.flixclusive.core.presentation.mobile.extensions

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Toggle the visibility of system bars (status bar and navigation bar).
 *
 * @param isVisible If true, show the system bars; if false, hide them.
 * */
@Suppress("DEPRECATION")
fun Activity.toggleSystemBars(isVisible: Boolean) {
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
