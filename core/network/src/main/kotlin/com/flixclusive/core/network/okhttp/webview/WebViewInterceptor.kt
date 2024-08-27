package com.flixclusive.core.network.okhttp.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.flixclusive.core.util.common.dispatcher.AppDispatchers.Companion.runOnMain
import com.flixclusive.core.util.exception.safeCall
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * An [OkHttpClient] interceptor that uses [WebView].
 *
 * @property context The application context.
 * @property interceptorName The name of the interceptor. For example, "Cloudflare Interceptor".
 * @property isHeadless Whether the WebView is running in headless mode.
 * @property mainThread The coroutine dispatcher for the main thread.
 *
 * @property cookieManager The [CookieManager] for managing cookies.
 * @property webStorage The [WebStorage] for managing web storage.
 * @property interceptor The [Interceptor] that intercepts the request and response.
 * @property shouldClearCache Whether to clear the cache.
 * @property shouldClearCookies Whether to clear the cookies.
 * @property shouldClearHistory Whether to clear the history.
 * @property shouldClearFormData Whether to clear the form data.
 * @property shouldClearWebStorage Whether to clear the web storage.
 * @property shouldClearSslPreferences Whether to clear the SSL preferences.
 * @property shouldClearAppCache Whether to clear the app cache.
 */
@Suppress("unused", "HasPlatformType", "MemberVisibilityCanBePrivate")
abstract class WebViewInterceptor(context: Context) : WebView(context) {
    companion object {
        fun OkHttpClient.addWebViewInterceptor(
            webViewInterceptor: WebViewInterceptor
        ): OkHttpClient {
            return newBuilder()
                .addInterceptor(webViewInterceptor.interceptor)
                .build()
        }
    }

    abstract val interceptorName: String
    abstract val isHeadless: Boolean

    open val shouldClearCache: Boolean = true
    open val shouldClearCookies: Boolean = true
    open val shouldClearHistory: Boolean = true
    open val shouldClearFormData: Boolean = true
    open val shouldClearWebStorage: Boolean = true
    open val shouldClearSslPreferences: Boolean = true
    open val shouldClearAppCache: Boolean = true

    protected val mainThread = Dispatchers.Main
    val cookieManager = CookieManager.getInstance()
    val webStorage = WebStorage.getInstance()

    internal val interceptor: Interceptor = Interceptor {
        val response = intercept(it)
        safeCall { destroy() }
        return@Interceptor response
    }

    abstract fun intercept(chain: Interceptor.Chain): Response

    @SuppressLint("SetJavaScriptEnabled")
    override fun loadUrl(
        url: String,
        headers: Map<String, String>
    ) {
        runOnMain {
            super.loadUrl(url, headers)
        }

        if (!isHeadless) {
            WebViewInterceptorManager.register(this)
        }
    }

    override fun loadUrl(url: String) {
        runOnMain {
            super.loadUrl(url)
        }

        if (!isHeadless) {
            WebViewInterceptorManager.register(this)
        }
    }

    override fun destroy() {
        if (isHeadless) {
            runOnMain {
                if (shouldClearCache) {
                    clearCache(true)
                }
                if (shouldClearCookies) {
                    cookieManager?.removeAllCookies(null)
                    cookieManager.flush()
                }
                if (shouldClearHistory) {
                    clearHistory()
                }
                if (shouldClearFormData) {
                    clearFormData()
                }
                if (shouldClearWebStorage) {
                    webStorage.deleteAllData()
                }
                if (shouldClearSslPreferences) {
                    clearSslPreferences()
                }

                stopLoading()
                onPause()
                super.destroy()
            }
        } else {
            WebViewInterceptorManager.destroy()
        }
    }
}