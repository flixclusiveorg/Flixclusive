package com.flixclusive.provider.webview

import com.flixclusive.model.provider.MediaLink

/**
 * A [ProviderWebView] callback interface for providers to communicate events related to media links.
 */
interface ProviderWebViewCallback {
    /**
     * Called when the WebView needs to destroy itself.
     *
     */
    suspend fun onStop()

    /**
     * Called when a media link has been loaded.
     *
     * @param link The loaded [MediaLink].
     *
     * @see MediaLink
     */
    fun onLinkLoaded(link: MediaLink)
}