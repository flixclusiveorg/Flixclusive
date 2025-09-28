package com.flixclusive.data.tmdb.repository.impl

import android.content.Context
import com.flixclusive.core.common.asset.AssetReader
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.Resource.Failure.Companion.toNetworkException
import com.flixclusive.data.tmdb.model.TMDBHomeCatalogs
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TMDBHomeCatalogRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val tmdbApiService: TMDBApiService,
        private val appDispatchers: AppDispatchers,
    ) : TMDBHomeCatalogRepository,
        AssetReader<TMDBHomeCatalogs> {
        private val gson by lazy { Gson() }
        private val cacheMutex = Mutex()
        private var cachedTMDBHomeCatalogs: TMDBHomeCatalogs? = null

        companion object {
            private const val HOME_CATALOGS_ASSET = "home_catalogs.json"
        }

        override suspend fun read(filePath: String): TMDBHomeCatalogs =
            withContext(appDispatchers.io) {
                context.assets.open(filePath).use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    gson.fromJson(jsonString, TMDBHomeCatalogs::class.java)
                }
            }

        private suspend fun getHomeCatalogsData(): TMDBHomeCatalogs {
            return cacheMutex.withLock {
                cachedTMDBHomeCatalogs ?: run {
                    val data = read(HOME_CATALOGS_ASSET)
                    cachedTMDBHomeCatalogs = data
                    data
                }
            }
        }

        /**
         * Retrieves all home catalogs data from the asset file.
         */
        override suspend fun getAllCatalogs(): TMDBHomeCatalogs {
            return getHomeCatalogsData()
        }

        override suspend fun getTrending(
            mediaType: String,
            timeWindow: String,
            page: Int,
        ): Resource<SearchResponseData<FilmSearchItem>> {
            return withContext(appDispatchers.io) {
                try {
                    val response = tmdbApiService.getTrending(
                        mediaType = mediaType,
                        timeWindow = timeWindow,
                        page = page,
                    )

                    Resource.Success(response)
                } catch (e: Exception) {
                    e.toNetworkException()
                }
            }
        }
    }
