package com.flixclusive.domain.downloads.util

import com.flixclusive.core.network.util.ProgressListener
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

internal class ProgressInterceptor(
    private val listener: ProgressListener,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return originalResponse
            .newBuilder()
            .body(ProgressResponseBody(originalResponse.body, listener))
            .build()
    }

    companion object {
        fun OkHttpClient.Builder.addProgressListener(listener: ProgressListener): OkHttpClient.Builder {
            return this.addNetworkInterceptor(ProgressInterceptor(listener))
        }
    }
}
