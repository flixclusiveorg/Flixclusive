package com.flixclusive.data.tmdb.repository.impl

import android.content.Context
import com.flixclusive.core.common.asset.AssetReader
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalogs
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TMDBDiscoverCatalogRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appDispatcher: AppDispatchers,
    ) : TMDBDiscoverCatalogRepository,
        AssetReader<TMDBDiscoverCatalogs> {
        private val gson by lazy { Gson() }

        private val cacheMutex = Mutex()
        private var cachedTMDBDiscoverCatalog: TMDBDiscoverCatalogs? = null

        companion object {
            private const val DISCOVER_CATALOGS_ASSET = "discover_catalogs.json"
        }

        override suspend fun read(filePath: String): TMDBDiscoverCatalogs =
            withContext(appDispatcher.io) {
                context.assets.open(filePath).use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    gson.fromJson(jsonString, TMDBDiscoverCatalogs::class.java)
                }
            }

        private suspend fun getTMDBDiscoverCatalog(): TMDBDiscoverCatalogs {
            return cacheMutex.withLock {
                cachedTMDBDiscoverCatalog ?: run {
                    val catalog = read(DISCOVER_CATALOGS_ASSET)
                    cachedTMDBDiscoverCatalog = catalog
                    catalog
                }
            }
        }

        override suspend fun getTvNetworks(): List<TMDBDiscoverCatalog> {
            val catalog = getTMDBDiscoverCatalog()
            return catalog.networks
        }

        override suspend fun getMovieCompanies(): List<TMDBDiscoverCatalog> {
            val catalog = getTMDBDiscoverCatalog()
            return catalog.companies
        }

        override suspend fun getTvGenres(): List<TMDBDiscoverCatalog> {
            return getGenres().filter { it.isForTv }
        }

        override suspend fun getMovieGenres(): List<TMDBDiscoverCatalog> {
            return getGenres().filter { it.isForMovie }
        }

        override suspend fun getGenres(): List<TMDBDiscoverCatalog> {
            val catalog = getTMDBDiscoverCatalog()
            return catalog.genres
        }

        override suspend fun getTv(): List<TMDBDiscoverCatalog> {
            val catalog = getTMDBDiscoverCatalog()
            return catalog.type.filter { it.isForTv }
        }

        override suspend fun getMovies(): List<TMDBDiscoverCatalog> {
            val catalog = getTMDBDiscoverCatalog()
            return catalog.type.filter { it.isForMovie }
        }
    }
