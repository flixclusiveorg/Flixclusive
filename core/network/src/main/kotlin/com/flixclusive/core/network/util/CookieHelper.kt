package com.flixclusive.core.network.util

import android.webkit.CookieManager

object CookieHelper {
    fun CookieManager.getValue(
        key: String,
        url: String
    ): String? {
        val cookies = getCookie(url)
        if (cookies == null || !cookies.contains("$key=")) {
            return null
        }

        return cookies.split("$key=")
            .getOrNull(1)
            ?.split(";")
            ?.getOrNull(0)
    }
}