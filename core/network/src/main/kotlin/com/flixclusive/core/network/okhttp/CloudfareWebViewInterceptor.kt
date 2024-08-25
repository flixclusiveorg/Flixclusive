package com.flixclusive.core.network.okhttp

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.flixclusive.core.network.util.CookieHelper.getValue
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.USER_AGENT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

private val ERROR_CODES = listOf(
    HttpsURLConnection.HTTP_FORBIDDEN /*403*/,
    HttpsURLConnection.HTTP_UNAVAILABLE /*503*/
)
private val SERVER_CHECK = listOf("cloudflare-nginx", "cloudflare")
private const val CLOUDFARE_COOKIE_KEY = "cf_clearance"

/**
 * If a CloudFare security verification redirection is detected, execute a
 * webView and retrieve the necessary headers.
 *
 * Reference code from [NovelDokusha](https://github.com/nanihadesuka/NovelDokusha/blob/ecca3fb4a62479d5187bbedee9c0efae81050f2c/networking/src/main/java/my/noveldokusha/network/interceptors/CloudfareVerificationInterceptor.kt#L33)
 */
@Suppress("unused")
class CloudfareWebViewInterceptor(
    private val context: Context
) : Interceptor {
    companion object {
        fun OkHttpClient.addCloudfareVerificationInterceptor(
            context: Context
        ): OkHttpClient {
            return newBuilder()
                .addInterceptor(
                    CloudfareWebViewInterceptor(context = context)
                )
                .build()
        }
    }

    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isCloudFare()) {
            return response
        }

        return lock.withLock {
            try {
                val cloudfareUrl = request.url.toString()
                val urlHost = request.url.domainWithScheme()

                val cookieManager = CookieManager.getInstance()
                    ?: throw Exception("[CF] Could not initialize cookie manager")

                response.close()

                val oldClearance = cookieManager.getValue(
                    key = CLOUDFARE_COOKIE_KEY,
                    url = cloudfareUrl
                )

                if (oldClearance != null) {
                    infoLog("[CF] Trying old cf_clearance...")
                    safeCall("[CF] Old cf_clearance might be outdated!") {
                        return@withLock chain.bypassChallenge(cookieManager)
                    }

                    runBlocking(Dispatchers.Main) {
                        cookieManager.removeAllCookies(null)
                        cookieManager.flush()
                    }
                }

                infoLog("[CF] Resolving with WebView...")
                runBlocking(Dispatchers.Default) {
                    withTimeoutOrNull(60.seconds) {
                        resolveWithWebView(request, cookieManager)
                    }
                } ?: throw Exception("[CF] WebView timed out after 60 seconds")

                infoLog("[CF] Using new cf_clearance cookie...")
                chain.bypassChallenge(cookieManager)
            } catch (e: CancellationException) {
                errorLog(e)
                throw e
            } catch (e: IOException) {
                errorLog(e)
                throw e
            } catch (e: Exception) {
                errorLog(e)
                throw IOException(e.message, e.cause)
            }
        }
    }

    private fun Response.isCloudFare(): Boolean {
        return code in ERROR_CODES
            && header("server") in SERVER_CHECK
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun resolveWithWebView(
        request: Request,
        cookieManager: CookieManager
    ): Boolean {
        val cloudfareUrl = request.url.toString()
        val headers = request
            .headers
            .toMultimap()
            .mapValues { it.value.firstOrNull() ?: "" }

        val mainThread = Dispatchers.Main
        withContext(mainThread) {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString = request.header("user-agent")
                    ?: USER_AGENT

                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {}
            }

            webView.loadUrl(cloudfareUrl, headers)
            CloudfareWebViewManager.updateWebView(webView)
        }

        while (true) {
            val clearance = cookieManager.getValue(
                key = CLOUDFARE_COOKIE_KEY,
                url = cloudfareUrl
            )

            if (clearance != null) {
                withContext(mainThread) {
                    CloudfareWebViewManager.destroyWebView()
                }
                return true
            }
        }
    }

    private fun Interceptor.Chain.bypassChallenge(cookieManager: CookieManager): Response {
        val request = request()
        val url = request.url.toString()
        val response = proceed(
            request.newBuilder()
                .addHeader("Cookie", cookieManager.getCookie(url))
                .build()
        )

        if (response.isCloudFare()) {
            response.close()
            throw Exception("[CF] Could not bypass verification")
        }

        return response
    }

    private fun HttpUrl.domainWithScheme(): String {
        return "$scheme://$host"
    }
}