package com.flixclusive.core.ui.common.util

import android.content.Context
import coil.request.ImageRequest

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

fun Context.buildImageUrl(
    imagePath: String?,
    imageSize: String = "w500",
): ImageRequest? {
    if(imagePath == null)
        return null

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
            } else {
                "$TMDB_IMAGE_BASE_URL$imageSize$imagePath"
            }
        )
    }

    return imageRequest
        .crossfade(true)
        .build()
}