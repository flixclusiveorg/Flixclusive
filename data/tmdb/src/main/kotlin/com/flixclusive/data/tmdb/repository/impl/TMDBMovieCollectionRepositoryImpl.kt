package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.data.tmdb.repository.TMDBMovieCollectionRepository
import com.flixclusive.model.film.TMDBCollection
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class TMDBMovieCollectionRepositoryImpl @Inject constructor(
    private val tmdbApiService: TMDBApiService,
    private val appDispatchers: AppDispatchers,
) : TMDBMovieCollectionRepository {

    override suspend fun getCollection(id: Int): Resource<TMDBCollection> {
        return withContext(appDispatchers.io) {
            try {
                val response = tmdbApiService.getCollection(id = id)

                Resource.Success(response)
            } catch (e: Exception) {
                e.toNetworkException()
            }
        }
    }
}
