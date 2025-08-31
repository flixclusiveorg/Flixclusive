package com.flixclusive.core.presentation.common.extensions

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import coil3.request.ImageRequest
import coil3.request.crossfade

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

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

/**
 * Extension function to build TMDB image URL to an [ImageRequest] for coil.
 *
 * @param imagePath The path/file suffix of the image URL
 * @param imageSize The image size you want TMDB to fetch for the image output
 *
 * @return Returns an [ImageRequest] if [imagePath] is valid, otherwise null.
 * */
fun Context.buildTMDBImageUrl(
    imagePath: String?,
    imageSize: String = "w500", // TODO: Convert to object
): ImageRequest? {
    if (imagePath == null) {
        return null
    }

    val imageRequest = ImageRequest.Builder(this)
    val pattern = "(https?://.+?/p/)([^/]+)(/.+)".toRegex()

    imageRequest.apply {
        data(
            if (imagePath.isEmpty()) {
                null
            } else if (pattern.matches(imagePath)) {
                val replacedUrl = pattern.replace(imagePath) { matchResult ->
                    val originalString = matchResult.groupValues[2]
                    matchResult.value.replace(originalString, imageSize)
                }
                replacedUrl
            } else if (imagePath.startsWith("/")) {
                "$TMDB_IMAGE_BASE_URL$imageSize$imagePath"
            } else {
                imagePath
            },
        )
    }

    return imageRequest
        .crossfade(true)
        .build()
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
