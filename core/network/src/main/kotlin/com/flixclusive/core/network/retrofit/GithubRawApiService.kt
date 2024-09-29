package com.flixclusive.core.network.retrofit

import com.flixclusive.core.util.common.GithubConstant.GITHUB_CONFIG_REPOSITORY
import com.flixclusive.core.util.common.GithubConstant.GITHUB_USERNAME
import com.flixclusive.model.configuration.catalog.HomeCatalogsData
import com.flixclusive.model.configuration.catalog.SearchCatalogsData
import retrofit2.http.GET

/**
 * 
 * A retrofit2 service for fetching app configs
 * from github using the pre-defined [GITHUB_USERNAME] and [GITHUB_CONFIG_REPOSITORY]
 * 
 * */
interface GithubRawApiService {
    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/home_items_config.json")
    suspend fun getHomeCatalogsConfig(): HomeCatalogsData

    @GET("$GITHUB_USERNAME/$GITHUB_CONFIG_REPOSITORY/main/search_items_config.json")
    suspend fun getSearchCatalogsConfig(): SearchCatalogsData

}