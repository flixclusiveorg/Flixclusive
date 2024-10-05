package com.flixclusive.data.tmdb.di

import com.flixclusive.core.network.di.TestRetrofitModule.getMockTMDBApiService
import com.flixclusive.data.configuration.di.test.TestAppConfigurationModule.getMockAppConfigurationManager
import com.flixclusive.data.tmdb.DefaultTMDBRepository
import com.flixclusive.data.tmdb.TMDBRepository
import okhttp3.OkHttpClient

object TestTmdbDataModule {
    fun getMockTMDBRepository(): TMDBRepository {
        val apiService = getMockTMDBApiService()
        val appConfigurationManager = getMockAppConfigurationManager()

        return DefaultTMDBRepository(
            tmdbApiService = apiService,
            okHttpClient = OkHttpClient(),
            configurationProvider = appConfigurationManager
        )
    }
}