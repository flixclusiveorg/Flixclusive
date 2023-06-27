package com.flixclusive.di

import android.app.Application
import com.flixclusive.common.Constants.CONSUMET_API_BASE_HOST
import com.flixclusive.common.Constants.TMDB_API_BASE_URL
import com.flixclusive.data.api.ConsumetApiService
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.api.utils.HostSelectionInterceptor
import com.flixclusive.domain.common.PaginatedSearchItemsDeserializer
import com.flixclusive.domain.common.SearchItemDeserializer
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.presentation.common.network.NetworkConnectivityObserver
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClientWithHostInterceptor(
        hostSelectionInterceptor: HostSelectionInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(hostSelectionInterceptor)

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideHostSelectionInterceptor() = HostSelectionInterceptor()

    @Provides
    @Singleton
    fun provideTMDBApiService(): TMDBApiService {
        val okHttpClient = OkHttpClient.Builder().build()
        val tmdbPageResponse = TMDBPageResponse<TMDBSearchItem>()

        val gson = GsonBuilder()
            .registerTypeAdapter(TMDBSearchItem::class.java, SearchItemDeserializer())
            .registerTypeAdapter(tmdbPageResponse::class.java, PaginatedSearchItemsDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
            .create(TMDBApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideConsumetApiService(
        okHttpClient: OkHttpClient
    ): ConsumetApiService {
        return Retrofit.Builder()
            .baseUrl("https://$CONSUMET_API_BASE_HOST/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ConsumetApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        application: Application,
        @MainDispatcher mainDispatcher: CoroutineDispatcher
    ) = NetworkConnectivityObserver(application, mainDispatcher)
}