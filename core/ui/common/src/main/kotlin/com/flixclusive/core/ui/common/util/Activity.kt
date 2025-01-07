package com.flixclusive.core.ui.common.util

import android.content.Context
import android.content.ContextWrapper

/**
 * Retrieves an instance of the specified [Activity] from the current [Context].
 *
 * This function iterates through [ContextWrapper] instances until it finds an
 * [Activity] of the desired type. If no matching [Activity] is found, an
 * [IllegalStateException] is thrown.
 *
 * @return An instance of the specified [Activity].
 * @throws IllegalStateException if no matching [Activity] is found.
 */
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