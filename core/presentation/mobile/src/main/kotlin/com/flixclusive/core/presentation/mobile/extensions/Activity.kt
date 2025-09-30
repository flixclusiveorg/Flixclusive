package com.flixclusive.core.presentation.mobile.extensions

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Toggle the visibility of system bars (status bar and navigation bar).
 *
 * @param isVisible If true, show the system bars; if false, hide them.
 * */
@Suppress("DEPRECATION")
fun Activity.toggleFullScreen(isVisible: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    if (isVisible) {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    } else {
        windowInsetsController.hide(WindowInsetsCompat.Type.ime())
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
