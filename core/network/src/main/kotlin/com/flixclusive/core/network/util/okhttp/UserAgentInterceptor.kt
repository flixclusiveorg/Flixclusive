package com.flixclusive.core.network.util.okhttp

import okhttp3.Interceptor
import okhttp3.Response

internal class UserAgentInterceptor(
    private val defaultUserAgent: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", defaultUserAgent)
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}