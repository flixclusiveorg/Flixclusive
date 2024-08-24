package com.flixclusive.core.network.okhttp

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *
 * A singleton object that manages the state of the cloudfare WebView dialog.
 *
 * */
object CloudfareWebViewManager {
    private val _webView = MutableStateFlow<WebView?>(null)
    val webView = _webView.asStateFlow()

    fun updateWebView(webView: WebView) {
        _webView.value?.stopLoading()
        _webView.value?.destroy()

        _webView.value = webView
    }

    fun destroyWebView() {
        _webView.value?.stopLoading()
        _webView.value?.destroy()

        _webView.value = null
    }
}