package com.flixclusive.core.presentation.common.extensions

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size

/**
 * Extension function to show a toast message.
 *
 * @param message The message to be displayed in the toast.
 * @param duration The duration for which the toast should be visible. Default is Toast.LENGTH_SHORT
 * */
fun Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_SHORT,
) {
    Toast.makeText(applicationContext, message, duration).show()
}

/**
 * Extension function to build an [ImageRequest] for loading an image using Coil.
 *
 * @param imagePath The path/file suffix of the image URL
 *
 * @return Returns an [ImageRequest] if [imagePath] is valid, otherwise null.
 * */
fun Context.buildImageRequest(
    imagePath: String?,
    imageSize: Size = Size.ORIGINAL,
): ImageRequest? {
    if (imagePath == null) {
        return null
    }

    val imageRequest = ImageRequest.Builder(this).apply {
        data(imagePath.ifEmpty { null })
        size(imageSize)
        crossfade(true)
    }

    return imageRequest.build()
}

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
        is Activity -> {
            this
        }

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
