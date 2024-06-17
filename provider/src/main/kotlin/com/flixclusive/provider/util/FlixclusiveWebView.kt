package com.flixclusive.provider.util

import android.content.Context
import android.webkit.WebView
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.model.tmdb.FilmDetails

abstract class FlixclusiveWebView(
    filmToScrape: FilmDetails,
    context: Context,
    callback: WebViewCallback,
    episodeData: Episode? = null,
) : WebView(context) {
    abstract suspend fun startScraping()
}