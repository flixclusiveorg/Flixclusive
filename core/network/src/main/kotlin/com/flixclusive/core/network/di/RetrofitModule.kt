package com.flixclusive.core.network.di

import com.flixclusive.core.network.retrofit.GithubApiService
import com.flixclusive.core.network.retrofit.GithubRawApiService
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.util.serializers.FilmSearchItemDeserializer
import com.flixclusive.core.network.util.serializers.PaginatedSearchItemsDeserializer
import com.flixclusive.core.network.util.serializers.TMDBMovieDeserializer
import com.flixclusive.core.network.util.serializers.TMDBTvShowDeserializer
import com.flixclusive.core.util.common.GithubConstant.GITHUB_API_BASE_URL
import com.flixclusive.core.util.common.GithubConstant.GITHUB_RAW_API_BASE_URL
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.google.gson.GsonBuilder
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
        val searchResponseData = SearchResponseData<FilmSearchItem>()

        val gson = GsonBuilder()
            .registerTypeAdapter(FilmSearchItem::class.java, FilmSearchItemDeserializer())
            .registerTypeAdapter(Movie::class.java, TMDBMovieDeserializer())
            .registerTypeAdapter(TvShow::class.java, TMDBTvShowDeserializer())
            .registerTypeAdapter(searchResponseData::class.java, PaginatedSearchItemsDeserializer())
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
    fun provideGithubRawApiService(
        client: OkHttpClient
    ): GithubRawApiService =
        Retrofit.Builder()
            .baseUrl(GITHUB_RAW_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GithubRawApiService::class.java)

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