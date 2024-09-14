package com.flixclusive.core.util.android

import android.content.Context
import android.content.ContextWrapper


inline fun <reified Activity : android.app.Activity> Context.getActivity(): Activity {
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
        "No proper activity instance was found!"
    }

    return activity
}