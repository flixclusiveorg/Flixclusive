package com.flixclusive.core.presentation.mobile.extensions

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
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
        val decorView = window.decorView
        val update = systemBarsUpdate(
            isVisible = isVisible,
            systemBarsVisible = ViewCompat
                .getRootWindowInsets(decorView)
                ?.isVisible(WindowInsetsCompat.Type.systemBars())
        )
        val windowInsetsController = WindowCompat.getInsetsController(window, decorView)
        windowInsetsController.systemBarsBehavior = update.behavior

        if (update.hideBeforeShow) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        if (isVisible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.ime())
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        if (update.reapplyInsets) {
            decorView.post { ViewCompat.requestApplyInsets(decorView) }
        }
        return
    }

    val state = if (!isVisible) {
        (
            window.decorView.systemUiVisibility
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    } else {
        (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    if (window.decorView.systemUiVisibility != state) {
        window.decorView.systemUiVisibility = state
    }
}

internal data class SystemBarsUpdate(
    val behavior: Int,
    val hideBeforeShow: Boolean,
    val reapplyInsets: Boolean
)

/**
 * @param systemBarsVisible Whether system bars are currently visible. Null root insets
 * count as not visible, which is the in-player back path.
 */
internal fun systemBarsUpdate(
    isVisible: Boolean,
    systemBarsVisible: Boolean?
): SystemBarsUpdate {
    if (!isVisible) {
        return SystemBarsUpdate(
            behavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            hideBeforeShow = false,
            reapplyInsets = false
        )
    }

    return SystemBarsUpdate(
        behavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT,
        hideBeforeShow = systemBarsVisible == true,
        reapplyInsets = true
    )
}
