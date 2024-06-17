package com.flixclusive.core.network.retrofit

import com.flixclusive.core.util.common.configuration.GITHUB_CONFIG_REPOSITORY
import com.flixclusive.core.util.common.configuration.GITHUB_USERNAME
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.tmdb.category.HomeCategoriesData
import com.flixclusive.model.tmdb.category.SearchCategoriesData
import retrofit2.http.GET

/**
 * 
 * A retrofit2 service for fetching app configs
 * from github using the pre-defined [GITHUB_USERNAME] and [GITHUB_CONFIG_REPOSITORY]
 * 
 * */
interface GithubRawApiService {
    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/home_items_config.json")
    suspend fun getHomeCategoriesConfig(): HomeCategoriesData

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/search_items_config.json")
    suspend fun getSearchCategoriesConfig(): SearchCategoriesData

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/app.json")
    suspend fun getAppConfig(): AppConfig
}