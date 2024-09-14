package com.flixclusive.domain.catalog

import com.flixclusive.core.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.exception.actualMessage
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.provider.ProviderApiRepository
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import javax.inject.Inject
import javax.inject.Singleton
import com.flixclusive.core.locale.R as LocaleR

@Singleton
class CatalogItemsProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val providerApiRepository: ProviderApiRepository
) {
    suspend operator fun invoke(
        catalog: Catalog,
        page: Int
    ): Resource<SearchResponseData<FilmSearchItem>> {
        return when (catalog) {
            is ProviderCatalog -> {
                return try {
                    val api = catalog.providerApi!!
                    val items = withIOContext {
                        api.getCatalogItems(
                            page = page,
                            catalog = catalog
                        )
                    }

                    Resource.Success(items)
                } catch (e: Exception) {
                    errorLog(e)
                    Resource.Failure(UiText.StringResource(LocaleR.string.failed_to_fetch_catalog_items_format_message, e.actualMessage))
                }
            }
            else -> tmdbRepository.paginateConfigItems(
                url = catalog.url,
                page = page
            )
        }
    }

    private val ProviderCatalog.providerApi: ProviderApi?
        get() = providerApiRepository.apiMap[providerName]
}