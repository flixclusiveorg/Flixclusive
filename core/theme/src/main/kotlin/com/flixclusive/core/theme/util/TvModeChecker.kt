package com.flixclusive.core.theme.util

import android.app.Activity
import android.app.UiModeManager
import android.content.res.Configuration
import androidx.activity.ComponentActivity

object TvModeChecker {
    fun Activity.isTvMode(): Boolean {
        val uiModeManager = getSystemService(ComponentActivity.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}