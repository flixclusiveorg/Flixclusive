package com.flixclusive.provider.webview

import android.content.Context
import androidx.annotation.MainThread
import com.flixclusive.core.util.webview.WebViewDriver
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode

@Suppress("unused")
@MainThread
abstract class ProviderWebView(
    context: Context,
) : WebViewDriver(context) {
    abstract suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode? = null,
        onLinkFound: (MediaLink) -> Unit,
    )
}