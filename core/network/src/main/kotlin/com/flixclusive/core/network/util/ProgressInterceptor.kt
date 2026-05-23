package com.flixclusive.core.network.util

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val listener = request.tag(ProgressListener::class.java)
            ?: return chain.proceed(request)

        val originalResponse = chain.proceed(request)
        return originalResponse
            .newBuilder()
            .body(ProgressResponseBody(originalResponse.body, listener))
            .build()
    }

    companion object {
        fun OkHttpClient.addProgressListener(): OkHttpClient {
            return newBuilder()
                .addNetworkInterceptor(ProgressInterceptor())
                .build()
        }
    }
}
