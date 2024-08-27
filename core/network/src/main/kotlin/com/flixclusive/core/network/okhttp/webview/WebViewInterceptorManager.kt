package com.flixclusive.core.network.okhttp.webview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *
 * A singleton object that can be used to access URLs and manipulate webpage/api responses through WebView instead of using OkHttp3.
 *
 * It handles if a [WebViewInterceptor] will be headless or not.
 *
 * */
object WebViewInterceptorManager {
    private val _webView = MutableStateFlow<WebViewInterceptor?>(null)
    val webView = _webView.asStateFlow()

    /**
     *
     * Registers a new [WebViewInterceptor] and sets it to the current [WebViewInterceptor].
     * */
    fun register(webView: WebViewInterceptor) {
        _webView.value?.stopLoading()
        _webView.value?.destroy()

        _webView.value = webView
    }

    /**
     *
     * Destroys current [WebViewInterceptor] and sets it to null.
     * */
    fun destroy() {
        _webView.value?.destroy()
        _webView.value = null
    }
}