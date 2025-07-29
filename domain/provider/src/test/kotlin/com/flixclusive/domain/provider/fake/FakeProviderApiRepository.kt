package com.flixclusive.domain.provider.fake

import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.provider.util.CollectionsOperation
import com.flixclusive.domain.provider.DEFAULT_FILM_ID
import com.flixclusive.domain.provider.GetMediaLinksUseCaseTest.Companion.createMockStream
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.filter.FilterList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeProviderApiRepository : ProviderApiRepository {
    private val apis = mutableMapOf<String, ProviderApi>()
    private val operations = MutableSharedFlow<CollectionsOperation.Map<String, ProviderApi>>()

    override fun observe(): SharedFlow<CollectionsOperation.Map<String, ProviderApi>> = operations.asSharedFlow()

    override fun getAll(): List<Pair<String, ProviderApi>> = apis.toList()

    override fun getApis(): List<ProviderApi> = apis.values.toList()

    override fun getApi(id: String): ProviderApi? = apis[id]

    override suspend fun addApiFromProvider(
        id: String,
        provider: Provider,
    ) {
        val mockApi = createMockProviderApi()
        apis[id] = mockApi
        operations.emit(CollectionsOperation.Map.Add(id, mockApi))
    }

    override suspend fun addApiFromId(id: String) {
        val mockApi = createMockProviderApi()
        apis[id] = mockApi
        operations.emit(CollectionsOperation.Map.Add(id, mockApi))
    }

    override suspend fun removeApi(id: String) {
        val removedApi = apis.remove(id)
        operations.emit(CollectionsOperation.Map.Remove(id, removedApi))
    }

    override suspend fun clearAll() {
        apis.clear()
    }

    fun addMockApi(id: String) {
        apis[id] = createMockProviderApi()
    }

    private fun createMockProviderApi(): ProviderApi {
        return object : ProviderApi() {
            override suspend fun getLinks(
                watchId: String,
                film: FilmMetadata,
                episode: Episode?,
                onLinkFound: (MediaLink) -> Unit,
            ) {
                if (watchId == DEFAULT_FILM_ID) {
                    onLinkFound(createMockStream())
                }
            }

            override suspend fun getMetadata(film: Film): FilmMetadata {
                return testFilm
            }

            override suspend fun search(
                title: String,
                page: Int,
                id: String?,
                imdbId: String?,
                tmdbId: Int?,
                filters: FilterList,
            ): SearchResponseData<FilmSearchItem> {
                return SearchResponseData(
                    results = listOf(
                        FilmSearchItem(
                            id = DEFAULT_FILM_ID,
                            title = "Test Film",
                            year = 2023,
                            posterImage = "https://example.com/test-film-poster.jpg",
                            homePage = null,
                            filmType = FilmType.MOVIE,
                            imdbId = "tt1234567",
                            tmdbId = 123456,
                            providerId = "test-provider-id",
                        ),
                    ),
                    page = page,
                    totalPages = 1,
                )
            }
        }
    }
}
