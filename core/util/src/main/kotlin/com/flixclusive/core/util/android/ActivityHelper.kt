package com.flixclusive.core.util.android

import android.content.Context
import android.content.ContextWrapper
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
        stringResource(id = R.string.null_player_context_error)
    }

    return activity
}