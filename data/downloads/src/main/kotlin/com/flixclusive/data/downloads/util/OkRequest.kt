package com.flixclusive.data.downloads.util

import okhttp3.Request

/** Builds a GET for [url] carrying [headers], plus whatever [block] adds. */
internal fun okRequest(
    url: String,
    headers: Map<String, String>,
    block: Request.Builder.() -> Unit = {},
): Request = Request
    .Builder()
    .url(url)
    .apply {
        headers.forEach { (name, value) -> addHeader(name, value) }
        block()
    }.build()
