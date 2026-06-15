package com.flixclusive.core.network.di

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.util.common.GithubConstant.GITHUB_API_BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RetrofitModule {
    @Provides
    @Singleton
    fun provideGithubApiService(
        client: OkHttpClient
    ): GithubApiService {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        return Retrofit
            .Builder()
            .client(client)
            .baseUrl(GITHUB_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GithubApiService::class.java)
    }
}
