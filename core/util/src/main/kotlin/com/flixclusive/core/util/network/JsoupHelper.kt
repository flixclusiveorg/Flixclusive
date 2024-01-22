package com.flixclusive.core.util.network

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body!!.string(), request.url.toString())
}