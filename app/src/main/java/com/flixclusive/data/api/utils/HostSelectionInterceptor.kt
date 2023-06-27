package com.flixclusive.data.api.utils

import com.flixclusive.common.Constants.CONSUMET_API_BASE_HOST
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class HostSelectionInterceptor : Interceptor {
    private var consumetHost: String = CONSUMET_API_BASE_HOST

    fun updateHost(newHost: String) {
        consumetHost = newHost
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val requestHost = request.url.host

        if(!requestHost.equals(other = consumetHost, ignoreCase = true)) {
            val newUrl = request.url.replaceHost(consumetHost)
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }

        return chain.proceed(request)
    }

    private fun HttpUrl.replaceHost(newHost: String): HttpUrl {
        return newBuilder()
            .host(newHost)
            .build()
    }
}