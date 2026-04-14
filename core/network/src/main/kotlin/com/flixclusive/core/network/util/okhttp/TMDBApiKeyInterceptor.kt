package com.flixclusive.core.network.util.okhttp

import okhttp3.Interceptor
import okhttp3.Response

private const val TMDB_API_KEY = "1865f43a0549ca50d341dd9ab8b29f49"

/**
 * Interceptor to add the TMDB API key to requests if it is not already present.
 * */
class TMDBApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val url = request.url
            .newBuilder()
            .apply {
                if (!request.url.queryParameterNames.contains("api_key")) {
                    addQueryParameter("api_key", TMDB_API_KEY)
                }
            }.build()

        val newRequest = request
            .newBuilder()
            .url(url)
            .build()

        return chain.proceed(newRequest)
    }
}
