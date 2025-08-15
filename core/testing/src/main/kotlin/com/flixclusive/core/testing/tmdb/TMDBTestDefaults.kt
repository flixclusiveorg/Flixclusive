package com.flixclusive.core.testing.tmdb

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.util.getSearchItemGson
import com.flixclusive.core.network.util.okhttp.TMDBApiKeyInterceptor
import com.flixclusive.core.testing.network.NetworkTestDefaults.createTestOkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Test configuration for creating actual TMDBApiService instances for integration testing.
 * This replaces mocked services with real HTTP clients for end-to-end testing.
 */
object TMDBTestDefaults {

    /**
     * Creates a real TMDBApiService instance for integration testing.
     * Uses actual HTTP client with reasonable timeouts for test environment.
     */
    fun createTMDBApiService(): TMDBApiService {
        val testOkHttpClient = createTestOkHttpClient()
            .newBuilder()
            .addInterceptor(TMDBApiKeyInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(getSearchItemGson()))
            .client(testOkHttpClient)
            .build()
            .create(TMDBApiService::class.java)
    }
}
