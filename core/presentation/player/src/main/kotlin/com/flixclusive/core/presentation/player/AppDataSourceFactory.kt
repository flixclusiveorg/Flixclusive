package com.flixclusive.core.presentation.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import okhttp3.OkHttpClient
import javax.inject.Inject

@OptIn(UnstableApi::class)
interface AppDataSourceFactory {
    val remote: CacheDataSource.Factory
    val local: DefaultDataSource.Factory

    /**
     * Sets the default request properties for the HTTP data source factory.
     *
     * @param properties A map of request properties to be set.
     * */
    fun setRequestProperties(properties: Map<String, String>)
}

@OptIn(UnstableApi::class)
internal class AppDataSourceFactoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val context: Context,
    private val cache: SimpleCache,
) : AppDataSourceFactory {
    private val httpDataSourceFactory by lazy {
        OkHttpDataSource
            .Factory(client)
            .setUserAgent(UserAgentManager.getRandomUserAgent())
    }

    override val remote by lazy {
        CacheDataSource
            .Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    override val local by lazy {
        DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    /**
     * Sets the default request properties for the HTTP data source factory.
     *
     * @param properties A map of request properties to be set.
     * */
    override fun setRequestProperties(properties: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(properties)
    }
}
