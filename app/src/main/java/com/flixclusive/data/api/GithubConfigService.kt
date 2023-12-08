package com.flixclusive.data.api

import com.flixclusive.domain.model.config.AppConfig
import com.flixclusive.domain.model.config.HomeCategoriesConfig
import com.flixclusive.domain.model.config.ProviderStatus
import com.flixclusive.domain.model.config.SearchCategoriesConfig
import retrofit2.http.GET

interface GithubConfigService {
    @GET("rhenwinch/flixclusive-config/main/home_items_config.json")
    suspend fun getHomeCategoriesConfig(): HomeCategoriesConfig

    @GET("rhenwinch/flixclusive-config/main/search_items_config.json")
    suspend fun getSearchCategoriesConfig(): SearchCategoriesConfig

    @GET("rhenwinch/flixclusive-config/main/app.json")
    suspend fun getAppConfig(): AppConfig

    @GET("rhenwinch/flixclusive-config/main/providers_config.json")
    suspend fun getProvidersStatus(): List<ProviderStatus>
}