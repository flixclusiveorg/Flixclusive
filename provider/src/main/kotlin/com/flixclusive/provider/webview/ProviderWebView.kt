package com.flixclusive.provider.webview

import android.content.Context
import androidx.annotation.MainThread
import com.flixclusive.core.util.webview.WebViewDriver
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.film.FilmDetails
import com.flixclusive.model.film.common.tv.Episode

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