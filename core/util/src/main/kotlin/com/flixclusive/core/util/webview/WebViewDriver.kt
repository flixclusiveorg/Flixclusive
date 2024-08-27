package com.flixclusive.core.util.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.flixclusive.core.util.common.dispatcher.AppDispatchers

/**
 *
 * An extension of the [WebView] class. It allows users to run the instance on headless or non-headless mode.
 *
 *
 * @property name The name of the WebView. For example, "Cloudflare Interceptor". This will be shown in the WebViewDriverDialog
 * @property isHeadless Whether the WebView is running in headless mode.
 *
 * @property cookieManager The [CookieManager] for managing cookies.
 * @property webStorage The [WebStorage] for managing web storage.
 * @property shouldClearCache Whether to clear the cache.
 * @property shouldClearCookies Whether to clear the cookies.
 * @property shouldClearHistory Whether to clear the history.
 * @property shouldClearFormData Whether to clear the form data.
 * @property shouldClearWebStorage Whether to clear the web storage.
 * @property shouldClearSslPreferences Whether to clear the SSL preferences.
 * @property shouldClearAppCache Whether to clear the app cache.
 *
 * @see WebViewDriverManager
 * */
@Suppress(
    "unused",
    "HasPlatformType",
    "MemberVisibilityCanBePrivate"
)
abstract class WebViewDriver(
    context: Context
) : WebView(context) {
    abstract val name: String
    abstract val isHeadless: Boolean

    open val shouldClearCache: Boolean = true
    open val shouldClearCookies: Boolean = true
    open val shouldClearHistory: Boolean = true
    open val shouldClearFormData: Boolean = true
    open val shouldClearWebStorage: Boolean = true
    open val shouldClearSslPreferences: Boolean = true
    open val shouldClearAppCache: Boolean = true

    val cookieManager get() = CookieManager.getInstance()
    val webStorage get() = WebStorage.getInstance()

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(
        url: String,
        headers: Map<String, String>
    ) {
        AppDispatchers.runOnMain {
            super.loadUrl(url, headers)
        }

        if (!isHeadless) {
            WebViewDriverManager.register(this)
        }
    }

    override fun loadUrl(url: String) {
        AppDispatchers.runOnMain {
            super.loadUrl(url)
        }

        if (!isHeadless) {
            WebViewDriverManager.register(this)
        }
    }

    override fun destroy() {
        if (isHeadless) {
            AppDispatchers.runOnMain {
                if (shouldClearCache) {
                    clearCache(true)
                }
                if (shouldClearCookies) {
                    cookieManager?.removeAllCookies(null)
                    cookieManager?.flush()
                }
                if (shouldClearHistory) {
                    clearHistory()
                }
                if (shouldClearFormData) {
                    clearFormData()
                }
                if (shouldClearWebStorage) {
                    webStorage?.deleteAllData()
                }
                if (shouldClearSslPreferences) {
                    clearSslPreferences()
                }

                stopLoading()
                onPause()
                super.destroy()
            }
        } else {
            WebViewDriverManager.destroy()
        }
    }
}