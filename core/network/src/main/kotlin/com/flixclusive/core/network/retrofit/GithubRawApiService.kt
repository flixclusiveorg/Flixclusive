package com.flixclusive.core.network.retrofit

import com.flixclusive.core.util.common.configuration.GITHUB_CONFIG_REPOSITORY
import com.flixclusive.core.util.common.configuration.GITHUB_USERNAME
import com.flixclusive.model.configuration.AppConfig
import com.flixclusive.model.configuration.HomeCategoriesConfig
import com.flixclusive.model.configuration.SearchCategoriesConfig
import retrofit2.http.GET

/**
 * 
 * A retrofit2 service for fetching app configs
 * from github using the pre-defined [GITHUB_USERNAME] and [GITHUB_CONFIG_REPOSITORY]
 * 
 * */
interface GithubRawApiService {
    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/home_items_config.json")
    suspend fun getHomeCategoriesConfig(): HomeCategoriesConfig

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/search_items_config.json")
    suspend fun getSearchCategoriesConfig(): SearchCategoriesConfig

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/app.json")
    suspend fun getAppConfig(): AppConfig
}