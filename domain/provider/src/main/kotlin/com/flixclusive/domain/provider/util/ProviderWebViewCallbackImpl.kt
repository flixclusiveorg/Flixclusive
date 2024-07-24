package com.flixclusive.domain.provider.util

import com.flixclusive.core.util.log.infoLog
import com.flixclusive.model.provider.CachedLinks
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.provider.webview.ProviderWebViewCallback

internal class ProviderWebViewCallbackImpl(
    private val cachedLinks: CachedLinks,
    private val onDestroy: (Throwable?) -> Unit,
) : ProviderWebViewCallback {
    override suspend fun onStop(error: Throwable?) {
        infoLog("Destroying WebView...")
        onDestroy(error)
    }

    override fun onLinkLoaded(link: MediaLink) {
        if (link.url.isBlank()) {
            return
        }

        infoLog("A link has been loaded...")
        cachedLinks.add(link)
    }
}