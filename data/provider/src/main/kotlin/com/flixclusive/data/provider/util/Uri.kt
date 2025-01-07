package com.flixclusive.data.provider.util

internal fun replaceLastAfterSlash(
    url: String,
    replacement: String,
): String {
    val slashIndex = url.lastIndexOf('/')
    return if (slashIndex != -1) {
        url.substring(0, slashIndex + 1) + replacement
    } else {
        url + replacement
    }
}
