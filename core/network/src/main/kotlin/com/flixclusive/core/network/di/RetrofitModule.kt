package com.flixclusive.core.network.di

import com.flixclusive.model.tmdb.TMDBPageResponse
import com.flixclusive.model.tmdb.TMDBSearchItem
import com.flixclusive.core.network.retrofit.FlixclusiveConfigurationService
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.PaginatedSearchItemsDeserializer
import com.flixclusive.core.network.util.SearchItemDeserializer
import com.flixclusive.core.util.common.Constants.GITHUB_BASE_URL
import com.flixclusive.core.util.common.Constants.TMDB_API_BASE_URL
import com.google.gson.GsonBuilder
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

object RetrofitModule {

    @Provides
    @Singleton
    internal fun provideTMDBApiService(
        client: OkHttpClient
    ): TMDBApiService {
        val tmdbPageResponse = TMDBPageResponse<TMDBSearchItem>()

        val gson = GsonBuilder()
            .registerTypeAdapter(TMDBSearchItem::class.java, SearchItemDeserializer())
            .registerTypeAdapter(tmdbPageResponse::class.java, PaginatedSearchItemsDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(TMDBApiService::class.java)
    }

    @Provides
    @Singleton
    internal fun provideAppConfigService(
        client: OkHttpClient
    ): FlixclusiveConfigurationService =
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(FlixclusiveConfigurationService::class.java)
}