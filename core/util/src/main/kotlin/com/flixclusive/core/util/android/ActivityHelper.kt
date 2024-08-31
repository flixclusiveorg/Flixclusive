package com.flixclusive.core.util.android

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.R


// https://stackoverflow.com/a/72501132/19371763
@Composable
inline fun <reified Activity : ComponentActivity> Context.getActivity(): Activity {
    val activity = when (this) {
        is Activity -> this
        else -> {
            var context = this
            while (context is ContextWrapper) {
                context = context.baseContext
                if (context is Activity) return context
            }
            null
        }
    }

    check(activity != null) {
        stringResource(id = R.string.context_null_error)
    }

    return activity
}

fun Activity.isTvMode(): Boolean {
    val uiModeManager = getSystemService(ComponentActivity.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}