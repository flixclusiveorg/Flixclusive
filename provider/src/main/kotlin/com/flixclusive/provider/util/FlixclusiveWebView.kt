package com.flixclusive.provider.util

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import com.flixclusive.core.util.network.USER_AGENT

abstract class FlixclusiveWebView(
    private val context: Context,
    private val callback: WebViewCallback
) : WebView(context) {
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = USER_AGENT

        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    abstract fun startScraping()
}