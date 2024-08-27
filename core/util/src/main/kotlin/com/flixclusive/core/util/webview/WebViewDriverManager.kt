package com.flixclusive.core.util.webview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *
 * A singleton object that can be used to access URLs and manipulate webpage/api responses through WebView instead of using OkHttp3.
 *
 * It handles if a [WebViewDriver] will be headless or not.
 *
 * Remember only one instance of [WebViewDriver] can be registered at a time.
 *
 * */
object WebViewDriverManager {
    private val _webView = MutableStateFlow<WebViewDriver?>(null)
    val webView = _webView.asStateFlow()

    /**
     *
     * Registers a new [WebViewDriver] and sets it to the current [WebViewDriver].
     * */
    fun register(webView: WebViewDriver) {
        destroy()
        _webView.value = webView
    }

    /**
     *
     * Destroys current [WebViewDriver] and sets it to null.
     * */
    fun destroy() {
        _webView.value?.destroy()
        _webView.value = null
    }
}