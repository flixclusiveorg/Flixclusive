package com.flixclusive.data.provider.di

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.flixclusive.core.network.di.TestRetrofitModule.getMockTMDBApiService
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

object TestProviderDataModule {
    private val tmdbApiService: TMDBApiService = getMockTMDBApiService()

    fun getMockProviderApiRepository(): ProviderApiRepository {
        val apiMap = getMockProviderApiMap()
        return mockk<ProviderApiRepository> {
            every { this@mockk.apiMap } returns apiMap
        }
    }

    fun getMockProviderApiMap(): SnapshotStateMap<String, ProviderApi> {
        val apiMap = mutableStateMapOf<String, ProviderApi>()

        repeat(5) {
            val providerName = "TheMovieDB-$it"
            val catalog = getDummyProviderCatalog(providerName = providerName)

            val providerApi = mockk<ProviderApi> {
                every { catalogs } returns listOf(catalog)
                coEvery { getCatalogItems(any()) } coAnswers {
                    tmdbApiService.get(catalog.url)
                }
            }

            apiMap[providerName] = providerApi
        }

        return apiMap
    }

    private fun getDummyProviderCatalog(providerName: String): ProviderCatalog {
        return ProviderCatalog(
            name = providerName,
            providerName = providerName,
            url = "https://api.themoviedb.org/3/trending/movie/day?language=en-US&api_key=8d6d91941230817f7807d643736e8a49",
            canPaginate = true
        )
    }
}