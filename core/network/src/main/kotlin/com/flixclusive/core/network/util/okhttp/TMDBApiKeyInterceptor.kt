package com.flixclusive.core.network.util.okhttp

import com.flixclusive.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

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
                    addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                }
            }.build()

        val newRequest = request
            .newBuilder()
            .url(url)
            .build()

        return chain.proceed(newRequest)
    }
}
