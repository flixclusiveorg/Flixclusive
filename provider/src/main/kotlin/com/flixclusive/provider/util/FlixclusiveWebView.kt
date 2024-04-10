package com.flixclusive.provider.util

import android.content.Context
import android.webkit.WebView
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode

abstract class FlixclusiveWebView(
    filmToScrape: Film,
    context: Context,
    callback: WebViewCallback,
    episodeData: TMDBEpisode? = null,
) : WebView(context) {
    abstract suspend fun startScraping()
}