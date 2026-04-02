package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.domain.catalog.R
import com.flixclusive.domain.catalog.usecase.PaginateItemsUseCase
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class PaginateItemsUseCaseImpl @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
) : PaginateItemsUseCase {
    override suspend operator fun invoke(
        catalog: Catalog,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return when (catalog) {
            is ProviderCatalog -> {
                try {
                    val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                    val api = providerRepository.getApi(
                        id = catalog.providerId,
                        ownerId = userId
                    )

                    if (api == null) {
                        warnLog("API for provider ${catalog.providerId} not found, failed to paginate items.")
                        return Resource.Failure(
                            UiText.StringResource(
                                R.string.provider_api_not_found_format_message,
                                catalog.providerId,
                            ),
                        )
                    }

                    val items = withContext(appDispatchers.io) {
                        api.getCatalogItems(
                            page = page,
                            catalog = catalog,
                        )
                    }

                    Resource.Success(items)
                } catch (e: Exception) {
                    errorLog(e)
                    Resource.Failure(
                        UiText.StringResource(
                            R.string.failed_to_fetch_catalog_items_format_message,
                            e.actualMessage,
                        ),
                    )
                }
            }

            else -> {
                tmdbFilmSearchItemsRepository.get(
                    url = catalog.url,
                    page = page,
                )
            }
        }
    }
}
