package com.flixclusive.provider.webview

import android.content.Context
import android.webkit.WebView
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode

@Suppress("unused")
abstract class ProviderWebView(
    context: Context,
    protected val film: FilmDetails,
    protected val episodeData: Episode? = null,
) : WebView(context) {
    abstract suspend fun getLinks(): List<MediaLink>
}