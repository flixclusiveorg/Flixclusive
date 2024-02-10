package com.flixclusive.core.util.network

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

/**
 * Parses the [Response] as HTML using Jsoup library and returns a [Document] object.
 *
 * @param html The HTML content to parse. If null, the response body content will be used.
 * @return A Jsoup [Document] object representing the parsed HTML.
 * @throws IOException If an I/O error occurs while reading the response body.
 */
fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body!!.string(), request.url.toString())
}
