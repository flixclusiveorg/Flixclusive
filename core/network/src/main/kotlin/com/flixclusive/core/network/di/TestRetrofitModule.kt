package com.flixclusive.core.network.di

import com.flixclusive.core.network.di.RetrofitModule.provideTMDBApiService
import com.flixclusive.core.network.retrofit.TMDBApiService
import okhttp3.OkHttpClient

object TestRetrofitModule {
    fun getMockTMDBApiService(): TMDBApiService {
        return provideTMDBApiService(OkHttpClient())
    }
}