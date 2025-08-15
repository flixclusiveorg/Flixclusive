package com.flixclusive.core.testing.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Configuration for network-related tests.
 *
 * This object can be used to define constants, utility functions,
 * */
object NetworkTestDefaults {
    /**
     * Creates a test OkHttpClient instance with reasonable timeouts.
     *
     * @return An [OkHttpClient] instance configured for testing.
     * */
    fun createTestOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
