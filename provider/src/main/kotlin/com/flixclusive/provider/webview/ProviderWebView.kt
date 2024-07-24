package com.flixclusive.provider.webview

import android.content.Context
import android.webkit.WebView
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode

@Suppress("unused")
abstract class ProviderWebView(
    context: Context,
    private val film: FilmDetails,
    private val callback: ProviderWebViewCallback,
    private val episodeData: Episode? = null,
) : WebView(context) {
    abstract suspend fun getLinks()
}