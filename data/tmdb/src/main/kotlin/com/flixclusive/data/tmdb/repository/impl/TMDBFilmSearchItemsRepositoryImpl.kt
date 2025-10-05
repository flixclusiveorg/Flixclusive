package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.data.tmdb.util.TMDBFilters.Companion.getMediaTypeFromInt
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

internal class TMDBFilmSearchItemsRepositoryImpl
    @Inject
    constructor(
        private val tmdbApiService: TMDBApiService,
        private val appDispatchers: AppDispatchers,
    ) : TMDBFilmSearchItemsRepository {
        override suspend fun search(
            query: String,
            page: Int,
            filter: Int,
        ): Resource<SearchResponseData<FilmSearchItem>> {
            return withContext(appDispatchers.io) {
                try {
                    if (query.isEmpty()) {
                        return@withContext Resource.Failure("Search query should not be empty!")
                    }

                    val response = tmdbApiService.search(
                        mediaType = getMediaTypeFromInt(filter),
                        page = page,
                        query = query,
                    )

                    Resource.Success(response)
                } catch (e: Exception) {
                    e.toNetworkException()
                }
            }
        }

        override suspend fun get(
            url: String,
            page: Int,
        ): Resource<SearchResponseData<FilmSearchItem>> {
            return withContext(appDispatchers.io) {
                val fullUrl = "$url&page=$page"

                try {
                    val response = tmdbApiService.get(fullUrl)
                    Resource.Success(response)
                } catch (e: HttpException) {
                    errorLog("Http Error (${e.code()}) on URL[$fullUrl]: ${e.response()}")
                    errorLog(e)
                    Resource.Failure(e.actualMessage)
                } catch (e: Exception) {
                    e.toNetworkException()
                }
            }
        }
    }
