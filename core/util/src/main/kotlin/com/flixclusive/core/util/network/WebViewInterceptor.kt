package com.flixclusive.core.util.network

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.annotation.MainThread
import com.flixclusive.core.util.webview.WebViewDriver
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * An [OkHttpClient] interceptor that uses [WebView].
 *
 * @property interceptorName The name of the interceptor. For example, "Cloudflare Interceptor".
 * @property isHeadless Whether the WebView is running in headless mode.
 * @property mainThread The coroutine dispatcher for the main thread.
 *
 * @property cookieManager The [CookieManager] for managing cookies.
 * @property webStorage The [WebStorage] for managing web storage.
 * @property interceptor The [Interceptor] that intercepts the request and response.
 */
@Suppress("unused")
@MainThread
abstract class WebViewInterceptor(context: Context) : WebViewDriver(context) {
    companion object {
        fun OkHttpClient.addWebViewInterceptor(
            webViewInterceptor: WebViewInterceptor
        ): OkHttpClient {
            return newBuilder()
                .addInterceptor(webViewInterceptor.interceptor)
                .build()
        }
    }

    internal val interceptor: Interceptor = Interceptor {
        return@Interceptor intercept(it)
            .also { destroy() }
    }

    abstract fun intercept(chain: Interceptor.Chain): Response
}