package com.flixclusive.core.network.di

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.util.getSearchItemGson
import com.flixclusive.core.network.util.okhttp.TMDBApiKeyInterceptor
import com.flixclusive.core.util.common.GithubConstant.GITHUB_API_BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RetrofitModule {

    @Provides
    @Singleton
    fun provideTMDBApiService(
        client: OkHttpClient
    ): TMDBApiService {
        return Retrofit.Builder()
            .baseUrl(TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(getSearchItemGson()))
            .client(
                client.newBuilder()
                    .addInterceptor(TMDBApiKeyInterceptor())
                    .build()
            )
            .build()
            .create(TMDBApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGithubApiService(
        client: OkHttpClient
    ): GithubApiService =
        Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GithubApiService::class.java)
}
