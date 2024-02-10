package com.flixclusive.core.util.network

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * Configures the [OkHttpClient.Builder] to ignore all SSL errors, allowing connections to be established
 * even if certificates are not trusted.
 * @return The configured [OkHttpClient.Builder].
 */
fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
    val naiveTrustManager = SSLTrustManager()

    val insecureSocketFactory = SSLContext.getInstance("SSL").apply {
        val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    sslSocketFactory(insecureSocketFactory, naiveTrustManager)
    hostnameVerifier { _, _ -> true }
    return this
}

/**
 * Implementation of [X509TrustManager] that accepts all certificates without verification.
 * @suppress("CustomX509TrustManager")
 */
@SuppressLint("CustomX509TrustManager")
class SSLTrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
}
