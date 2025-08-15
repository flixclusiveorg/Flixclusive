package com.flixclusive.domain.tmdb.usecase.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.network.retrofit.TMDB_API_KEY
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.getSearchItemGson
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.domain.tmdb.usecase.PaginateTMDBCatalogUseCase
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

internal class PaginateTMDBCatalogUseCaseImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appDispatchers: AppDispatchers
) : PaginateTMDBCatalogUseCase {
    private val gson by lazy { getSearchItemGson() }

    override suspend fun invoke(
        url: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return withContext(appDispatchers.io) {
            val fullUrl = "$TMDB_API_BASE_URL$url&page=$page&api_key=$TMDB_API_KEY"

            try {
                val request = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body.string()

                    val searchResponse = gson.fromJson(
                        responseBody,
                        SearchResponseData::class.java
                    ) as SearchResponseData<FilmSearchItem>

                    Resource.Success(searchResponse)
                } else {
                    val errorMessage = "HTTP ${response.code}: ${response.message}"
                    errorLog("Http Error (${response.code}) on URL[$fullUrl]: ${response.message}")
                    Resource.Failure(errorMessage)
                }
            } catch (e: Exception) {
                errorLog("Error executing catalog query for URL[$fullUrl]")
                Resource.Failure(e.actualMessage)
            }
        }
    }
}
