package com.flixclusive.core.network.retrofit

import com.flixclusive.core.model.config.AppConfig
import com.flixclusive.core.model.config.HomeCategoriesConfig
import com.flixclusive.core.model.config.ProviderStatus
import com.flixclusive.core.model.config.SearchCategoriesConfig
import retrofit2.http.GET

const val GITHUB_USERNAME = "rhenwinch"
const val GITHUB_CONFIG_REPOSITORY = "flixclusive-config"

/**
 * 
 * A retrofit2 service for fetching app configs
 * from github using the pre-defined [GITHUB_USERNAME] and [GITHUB_CONFIG_REPOSITORY]
 * 
 * */
interface FlixclusiveConfigurationService {
    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/home_items_config.json")
    suspend fun getHomeCategoriesConfig(): HomeCategoriesConfig

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/search_items_config.json")
    suspend fun getSearchCategoriesConfig(): SearchCategoriesConfig

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/app.json")
    suspend fun getAppConfig(): AppConfig

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/providers_config.json")
    suspend fun getProvidersStatus(): List<ProviderStatus>
}